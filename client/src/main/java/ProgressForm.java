import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class ProgressForm {
    private final Stage dialogStage;
    private final ProgressBar pb = new ProgressBar();
    private final ProgressIndicator pin = new ProgressIndicator();
    private final Button okBtn = new Button("OK");
    private final Button cancelBtn = new Button("Cancel");

    public ProgressForm(String text) {
        dialogStage = new Stage();
        dialogStage.initStyle(StageStyle.UNDECORATED);
        dialogStage.setResizable(false);
        dialogStage.initModality(Modality.APPLICATION_MODAL);

        Label label = new Label();
        label.setText("Upload File: " + text);

        pb.setProgress(0);
        pin.setProgress(0);

        final VBox vBox = new VBox();
        vBox.setSpacing(5);
        vBox.setAlignment(Pos.CENTER);
        vBox.getChildren().add(pb);
        vBox.getChildren().add(pin);
        vBox.getChildren().add(label);
        final HBox hBox = new HBox();
        hBox.setSpacing(50);
        hBox.setAlignment(Pos.BOTTOM_CENTER);
        hBox.getChildren().addAll(okBtn, cancelBtn);
        okBtn.setVisible(false);
        vBox.getChildren().add(hBox);

        Scene scene = new Scene(vBox);
        dialogStage.setScene(scene);

        okBtn.setOnAction(event -> dialogStage.close());
    }

    public void setProgress(double progress) {
        pb.setProgress(progress);
        pin.setProgress(progress);
        if (pin.getProgress() == 1) {
            okBtn.setVisible(true);
            cancelBtn.setDisable(true);
            cancelBtn.setVisible(false);
        }
    }

    public void activateProgressBar()  {
        dialogStage.show();
    }

    public Button getCancelBtn() {
        return cancelBtn;
    }

    public Stage getDialogStage() {
        return dialogStage;
    }
}
