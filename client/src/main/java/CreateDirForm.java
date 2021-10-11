import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class CreateDirForm {
    private final Stage dialogStage;
    private TextField textField = new TextField("");
    private Label label = new Label("New Directory");

    public CreateDirForm() {
        dialogStage = new Stage();
        dialogStage.initStyle(StageStyle.UTILITY);
        dialogStage.setResizable(false);
        dialogStage.initModality(Modality.APPLICATION_MODAL);
        dialogStage.setWidth(300);

        final VBox vBox = new VBox();
        vBox.setSpacing(5);
        vBox.setAlignment(Pos.CENTER);
        vBox.getChildren().add(label);
        vBox.getChildren().add(textField);

        Scene scene = new Scene(vBox);
        dialogStage.setScene(scene);

    }

    public void activateForm()  {
        dialogStage.show();
    }

    public void closeForm() {
        dialogStage.close();
    }

    public Label getLabel() {
        return label;
    }

    public TextField getTextField() {
        return textField;
    }
}
