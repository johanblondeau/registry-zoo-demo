package fr.leroymerlin.demo.dto;

public class FakeApplication {

    private String name;
    private String url;
    private int priority;

    public FakeApplication() {
        super();
    }

    public FakeApplication(String name, String url, int priority) {
        this.name = name;
        this.url = url;
        this.priority = priority;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }
}
