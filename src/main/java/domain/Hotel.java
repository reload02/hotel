package domain;

public class Hotel {
    private String name;
    private String postalCode;
    private String hostId;

    public Hotel(String name, String postalCode, String hostId) {
        this.name = name;
        this.postalCode = postalCode;
        this.hostId = hostId;
    }

    public String getName() {
        return name;
    }

    public String normalizedNameForSearch() {
        return name.replace(" ", "").toLowerCase();
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public void setPostalCode(String postalCode) {
        this.postalCode = postalCode;
    }

    public String getHostId() {
        return hostId;
    }

    public String getHostIdKey() {
        return hostId.toLowerCase();
    }
}
