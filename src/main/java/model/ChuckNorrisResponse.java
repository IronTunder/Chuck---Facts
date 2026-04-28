package model;

public class ChuckNorrisResponse {
    // Nome allineato al JSON di chucknorris.io: Gson valorizza il campo via reflection.
    private String value;

    public String value() {
        return value;
    }
}
