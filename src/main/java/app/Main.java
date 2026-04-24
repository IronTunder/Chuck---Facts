package app;

import gui.MainFrame;
import javafx.application.Application;
import javafx.stage.Stage;

public class Main extends Application {
    @Override
    public void start(Stage stage) {
        MainFrame mainFrame = new MainFrame();
        mainFrame.show(stage);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
