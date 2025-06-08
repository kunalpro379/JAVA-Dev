package designpatterns;

import java.util.*;
import java.util.concurrent.*;

// Singleton Pattern
class Configuration {
    private static volatile Configuration instance;
    private Map<String, String> settings;

    private Configuration() {
        settings = new ConcurrentHashMap<>();
    }

    public static Configuration getInstance() {
        if (instance == null) {
            synchronized (Configuration.class) {
                if (instance == null) {
                    instance = new Configuration();
                }
            }
        }
        return instance;
    }

    public void setSetting(String key, String value) {
        settings.put(key, value);
    }

    public String getSetting(String key) {
        return settings.get(key);
    }
}

// Factory Pattern
interface NotificationService {
    void sendNotification(String message);
}

class EmailNotification implements NotificationService {
    @Override
    public void sendNotification(String message) {
        System.out.println("Sending email: " + message);
    }
}

class SMSNotification implements NotificationService {
    @Override
    public void sendNotification(String message) {
        System.out.println("Sending SMS: " + message);
    }
}

class NotificationFactory {
    public static NotificationService createNotification(String channel) {
        switch (channel.toLowerCase()) {
            case "email":
                return new EmailNotification();
            case "sms":
                return new SMSNotification();
            default:
                throw new IllegalArgumentException("Unknown channel " + channel);
        }
    }
}

// Observer Pattern
interface Observer {
    void update(String event);
}

interface Subject {
    void registerObserver(Observer observer);
    void removeObserver(Observer observer);
    void notifyObservers(String event);
}

class EventManager implements Subject {
    private List<Observer> observers = new ArrayList<>();

    @Override
    public void registerObserver(Observer observer) {
        observers.add(observer);
    }

    @Override
    public void removeObserver(Observer observer) {
        observers.remove(observer);
    }

    @Override
    public void notifyObservers(String event) {
        for (Observer observer : observers) {
            observer.update(event);
        }
    }
}

// Strategy Pattern
interface PaymentStrategy {
    void pay(double amount);
}

class CreditCardPayment implements PaymentStrategy {
    private String cardNumber;

    public CreditCardPayment(String cardNumber) {
        this.cardNumber = cardNumber;
    }

    @Override
    public void pay(double amount) {
        System.out.println("Paid $" + amount + " using credit card " + cardNumber);
    }
}

class PayPalPayment implements PaymentStrategy {
    private String email;

    public PayPalPayment(String email) {
        this.email = email;
    }

    @Override
    public void pay(double amount) {
        System.out.println("Paid $" + amount + " using PayPal account " + email);
    }
}

// Decorator Pattern
interface Coffee {
    double getCost();
    String getDescription();
}

class SimpleCoffee implements Coffee {
    @Override
    public double getCost() {
        return 1.0;
    }

    @Override
    public String getDescription() {
        return "Simple coffee";
    }
}

abstract class CoffeeDecorator implements Coffee {
    protected Coffee decoratedCoffee;

    public CoffeeDecorator(Coffee coffee) {
        this.decoratedCoffee = coffee;
    }

    @Override
    public double getCost() {
        return decoratedCoffee.getCost();
    }

    @Override
    public String getDescription() {
        return decoratedCoffee.getDescription();
    }
}

class MilkDecorator extends CoffeeDecorator {
    public MilkDecorator(Coffee coffee) {
        super(coffee);
    }

    @Override
    public double getCost() {
        return super.getCost() + 0.5;
    }

    @Override
    public String getDescription() {
        return super.getDescription() + " with milk";
    }
}

// Builder Pattern
class Computer {
    private String cpu;
    private String ram;
    private String storage;
    private String gpu;

    private Computer(Builder builder) {
        this.cpu = builder.cpu;
        this.ram = builder.ram;
        this.storage = builder.storage;
        this.gpu = builder.gpu;
    }

    public static class Builder {
        private String cpu;
        private String ram;
        private String storage;
        private String gpu;

        public Builder cpu(String cpu) {
            this.cpu = cpu;
            return this;
        }

        public Builder ram(String ram) {
            this.ram = ram;
            return this;
        }

        public Builder storage(String storage) {
            this.storage = storage;
            return this;
        }

        public Builder gpu(String gpu) {
            this.gpu = gpu;
            return this;
        }

        public Computer build() {
            return new Computer(this);
        }
    }

    @Override
    public String toString() {
        return "Computer{cpu='" + cpu + "', ram='" + ram + 
               "', storage='" + storage + "', gpu='" + gpu + "'}";
    }
}

// Command Pattern
interface Command {
    void execute();
    void undo();
}

class Light {
    private boolean isOn = false;
    
    public void turnOn() {
        isOn = true;
        System.out.println("Light is on");
    }
    
    public void turnOff() {
        isOn = false;
        System.out.println("Light is off");
    }
}

class LightOnCommand implements Command {
    private Light light;

    public LightOnCommand(Light light) {
        this.light = light;
    }

    @Override
    public void execute() {
        light.turnOn();
    }

    @Override
    public void undo() {
        light.turnOff();
    }
}

class RemoteControl {
    private Command command;

    public void setCommand(Command command) {
        this.command = command;
    }

    public void pressButton() {
        command.execute();
    }

    public void pressUndo() {
        command.undo();
    }
}

public class DesignPatternsDemo {
    public static void main(String[] args) {
        // Demonstrate Singleton
        Configuration config = Configuration.getInstance();
        config.setSetting("theme", "dark");
        System.out.println("Theme: " + config.getSetting("theme"));

        // Demonstrate Factory
        NotificationService emailService = NotificationFactory.createNotification("email");
        emailService.sendNotification("Hello!");

        // Demonstrate Observer
        EventManager eventManager = new EventManager();
        Observer logger = event -> System.out.println("Log: " + event);
        eventManager.registerObserver(logger);
        eventManager.notifyObservers("New event occurred");

        // Demonstrate Strategy
        PaymentStrategy creditCard = new CreditCardPayment("1234-5678-9012-3456");
        PaymentStrategy payPal = new PayPalPayment("user@example.com");
        creditCard.pay(100.0);
        payPal.pay(50.0);

        // Demonstrate Decorator
        Coffee coffee = new SimpleCoffee();
        coffee = new MilkDecorator(coffee);
        System.out.println(coffee.getDescription() + " costs $" + coffee.getCost());

        // Demonstrate Builder
        Computer computer = new Computer.Builder()
            .cpu("Intel i7")
            .ram("16GB")
            .storage("512GB SSD")
            .gpu("RTX 3080")
            .build();
        System.out.println(computer);

        // Demonstrate Command
        Light light = new Light();
        Command lightOn = new LightOnCommand(light);
        RemoteControl remote = new RemoteControl();
        remote.setCommand(lightOn);
        remote.pressButton();
        remote.pressUndo();
    }
}