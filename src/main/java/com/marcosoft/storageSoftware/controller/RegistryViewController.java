package com.marcosoft.storageSoftware.controller;

import com.marcosoft.storageSoftware.util.SceneSwitcher;
import com.marcosoft.storageSoftware.util.WindowShowing;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TableView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

@Controller
public class RegistryViewController implements Initializable {

    private final WindowShowing windowShowing;

    @FXML
    private TableView tblView;
    @FXML
    private HBox vboxSlogan1;

    public RegistryViewController(){
        windowShowing= new WindowShowing();
    }

    @Autowired
    private SceneSwitcher sceneSwitcher;

    @FXML 
    private void switchToSupport(ActionEvent event) throws IOException {
        sceneSwitcher.setRoot(event, "/supportView.fxml");
        windowShowing.closeAllWindows();
    }

    @FXML
    private void switchToExistency(ActionEvent event) throws IOException {
        sceneSwitcher.setRoot(event, "/stockView.fxml");
        windowShowing.closeAllWindows();
    }
    
   @FXML
    private void displayConfigurationView(ActionEvent event) throws IOException {
        String errorMessage= "Ya hay una ventana de Configuración abierta";
        String fxmlPath="/configurationView.fxml";
        int aux=3;
        windowShowing.displayAssistance(windowShowing.isConfigurationShowing(), fxmlPath, errorMessage, aux);
    } 
    
    @FXML
    private void displayRegistryfilterView(ActionEvent event) throws IOException{
        String errorMessage= "Ya hay una ventana de filtros para registros abierta";
        String fxmlPath="/registryFilterView.fxml";
        int aux=4;
        windowShowing.displayAssistance(windowShowing.isRegistryFilterViewShowing(), fxmlPath, errorMessage, aux);
    }
    
    @FXML
    private void selected(MouseEvent event){
        
    }
    
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // TODO
    }

    @FXML
    public void switchToWallet(ActionEvent actionEvent) {
    }
}
