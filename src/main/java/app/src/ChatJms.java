package app.src;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import org.apache.activemq.ActiveMQConnectionFactory;

import javax.jms.*;
import java.io.*;

public class ChatJms extends Application {

    //jms
    private Session session;
    private MessageProducer messageProducer;
    private Connection connection;
    //top content
    private Label labelCode = new Label("Code :");
    private TextField textFieldCode = new TextField();
    private Label labelHost = new Label("Host :");
    private TextField textFieldHost =  new TextField();
    private Label labelPort = new Label("Port :");
    private TextField textFieldPort = new TextField();
    private Button buttonConnect =new Button();

    //center content
    private Label labelTo = new Label("To :");
    private TextField textFieldTo =new TextField();
    private Label labelMessage =new Label("Message");
    private Button buttonSend = new Button();
    private TextArea textFieldMessage =new TextArea();
    private Label labelImage =new Label("Image");
    private File file =new File("images");
    private ObservableList<String> listImages = FXCollections.observableArrayList(file.list());
    private ComboBox<String> comboBoxImages =new ComboBox(listImages);
    private Button buttonSendImage =new Button();
    private ObservableList<String> listMessages= FXCollections.observableArrayList();
    private ListView<String> listViewMessages =new ListView<>(listMessages);
    private ImageView imageView = new ImageView();


    public static void main(String[] args) {
        Application.launch(args);
    }

    @Override
    public void start(Stage window) throws Exception {
        window.setTitle("Chat Jms");
        window.setWidth(800);
        window.setHeight(600);
        BorderPane root = new BorderPane();
        root.setTop(createTop());
        root.setCenter(createCenter());
        Scene scene =new Scene(root);
        window.setScene(scene);
        window.show();
        window.setOnCloseRequest(event -> {
            try {
                session.close();
                connection.close();
            } catch (JMSException e) {
                e.printStackTrace();
            }
        });
    }

    private Pane createTop() {
        HBox hBox =new HBox();
        hBox.setStyle("-fx-background-color: #ffa519");
        hBox.setPrefHeight(60);
        hBox.setSpacing(5);
        hBox.setAlignment(Pos.CENTER);

        textFieldCode.setPromptText("code");
        textFieldHost.setPromptText("host");
        textFieldHost.setText("localhost");
        textFieldPort.setPromptText("port");
        textFieldPort.setText("61616");
        buttonConnect.setText("Connect");
        hBox.getChildren().addAll(labelCode, textFieldCode,labelHost,textFieldHost,
                labelPort, textFieldPort, buttonConnect);

        buttonConnect.setOnAction((event -> {
            String code = textFieldCode.getText();
            String host = textFieldHost.getText();
            int port =Integer.parseInt(textFieldPort.getText());

            ConnectionFactory connectionFactory =
                    new ActiveMQConnectionFactory("tcp://" +host+ ":"+port);
            try {
                connection = connectionFactory.createConnection();
                connection.start();
                session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
                Destination destination =session.createTopic("chatTopic");
                MessageConsumer consumer =session.createConsumer(destination, "code='"+code+"'");
                messageProducer = session.createProducer(destination);
                messageProducer.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
                consumer.setMessageListener( message -> {
                        if(message instanceof TextMessage) {
                            TextMessage textMessage = (TextMessage) message;
                            try {
                                listMessages.add(textMessage.getText());
                            } catch (JMSException e) {
                                e.printStackTrace();
                            }
                        } else {
                            if( message instanceof StreamMessage) {
                                StreamMessage streamMessage = (StreamMessage) message;
                                try {
                                    String namePhoto = streamMessage.readString();
                                    int size = streamMessage.readInt();
                                    byte[] data = new byte[size];
                                    streamMessage.readBytes(data);
                                    ByteArrayInputStream byteArrayInputStream= new ByteArrayInputStream(data);
                                    Image image =new Image(byteArrayInputStream);
                                    imageView.setImage(image);
                                } catch (JMSException e) {
                                    e.printStackTrace();
                                }

                            }
                        }
                });
                hBox.setDisable(true);
            } catch (JMSException e) {
                e.printStackTrace();
            }
        }));
        return hBox;
    }

    private Pane createCenter() {
        GridPane gridPane =new GridPane();
        HBox hBox = new HBox();
        VBox vBox =new VBox();
        vBox.setSpacing(20);
        vBox.setStyle("-fx-padding: 20");
        vBox.getChildren().addAll(gridPane, hBox);

        //add elements to gridPane
        gridPane.add(labelTo, 1, 1);
        textFieldTo.setPromptText("to");
        gridPane.add(textFieldTo, 2, 1);
        gridPane.add(labelMessage, 1, 2);
        textFieldMessage.setPromptText("message ...");
        textFieldMessage.setPrefSize(280, 50);
        textFieldMessage.setWrapText(true);
        gridPane.add(textFieldMessage, 2, 2);
        buttonSend.setText("Send Message");
        gridPane.add(buttonSend, 3, 2);
        gridPane.add(labelImage, 1, 3);
        comboBoxImages.getSelectionModel().select(0);
        gridPane.add(comboBoxImages, 2, 3);
        buttonSendImage.setText("Send Image");
        gridPane.add(buttonSendImage, 3,3);
        gridPane.setHgap(20);
        gridPane.setVgap(20);

        hBox.setSpacing(10);
        File file = new File("images/"+ comboBoxImages.getSelectionModel().getSelectedItem());
        Image image = new Image(file.toURI().toString());
        imageView.setImage(image);
        hBox.getChildren().addAll(listViewMessages, imageView);

        comboBoxImages.getSelectionModel().selectedItemProperty()
                .addListener((observable, oldValue, newValue) -> {
            File file2 = new File("images/"+ newValue);
            Image image2 = new Image(file2.toURI().toString());
            imageView.setImage(image2); });
        buttonSend.setOnAction(event -> {
            try {
                TextMessage textMessage = session.createTextMessage();
                textMessage.setText(textFieldMessage.getText());
                textMessage.setStringProperty("code", textFieldTo.getText());
                messageProducer.send(textMessage);
            } catch (JMSException e) {
                e.printStackTrace();
            }
        });
        buttonSendImage.setOnAction(event -> {
            try {
                StreamMessage streamMessage = session.createStreamMessage();
                String selectedImageName = comboBoxImages.getSelectionModel().getSelectedItem();
                File file1 = new File("images/" + selectedImageName);
                FileInputStream fileInputStream =new FileInputStream(file1);
                byte[] data = new byte[(int) file1.length()];
                fileInputStream.read(data);
                streamMessage.setStringProperty("code", textFieldTo.getText());
                streamMessage.writeString(selectedImageName);
                streamMessage.writeInt(data.length);
                streamMessage.writeBytes(data);
                messageProducer.send(streamMessage);

            } catch (JMSException | IOException e ) {
                e.printStackTrace();
            }
        });
        return vBox;
    }

}
