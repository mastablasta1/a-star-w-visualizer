package pl.edu.agh.idziak.asw.visualizer.gui.root;

import javafx.scene.control.Alert;

/**
 * Created by Tomasz on 27.08.2016.
 */
public class DialogDisplay {

    public void displayError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setContentText(message);
        alert.showAndWait();
    }

    public void displayException(String msg, Throwable e) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(e.getClass().getSimpleName());
        alert.setContentText(msg + ": " + e.getMessage());
        alert.showAndWait();
    }
}
