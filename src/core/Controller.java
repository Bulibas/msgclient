package core;

import com.sun.scenario.effect.impl.sw.sse.SSEBlend_SRC_OUTPeer;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class Controller implements Initializable {

    @FXML
    private ListView usersListView;

    private ObservableList<Text> usersList;

    @FXML
    public ListView messagesTextArea;

    private ObservableList<Text> messagesList;

    @FXML
    public TextField inputTextField;

    private ProtocolClient client;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        usersList = FXCollections.observableArrayList();
        usersListView.setItems(usersList);

        messagesList = FXCollections.observableArrayList();
        messagesTextArea.setItems(messagesList);

        inputTextField.setOnKeyPressed(new EventHandler<KeyEvent>() {
            @Override
            public void handle(KeyEvent event) {
                if(event.getCode() == KeyCode.ENTER) {
                    try {
                        client.sendMessageFromGUI(inputTextField.getText());
                        inputTextField.setText("");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        });

    }

    public void setClient(ProtocolClient client) {
        this.client = client;
    }

    public void addUserToList(Text t) {
        usersList.add(t);
    }

    public void addMessageToList(Text t) {
        messagesList.addAll(t);
    }

}
