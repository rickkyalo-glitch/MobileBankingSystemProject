package wire;

public class User {
    private final int id;
    private final String name;
    private final String idNumber;
    private final String phoneNumber;
    private String pinHash;
    private double balance;

    public User(int id, String name, String idNumber, String phoneNumber, String pinHash, double balance) {
        this.id = id;
        this.name = name;
        this.idNumber = idNumber;
        this.phoneNumber = phoneNumber;
        this.pinHash = pinHash;
        this.balance = balance;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getIdNumber() {
        return idNumber;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public String getPinHash() {
        return pinHash;
    }

    public double getBalance() {
        return balance;
    }

    public void setBalance(double balance) {
        this.balance = balance;
    }

    public void setPinHash(String pinHash) {
        this.pinHash = pinHash;
    }
}
