import java.util.*;
import java.text.SimpleDateFormat;

public class OrderProcessor {
    public static int GLOBAL_DISCOUNT = 5;
    public List<Order> orders = new ArrayList<>();

    public void processOrders(List<Order> inputOrders, boolean notifyCustomers, int retryCount) {
        if (inputOrders == null) return;

        for (Order o : inputOrders) {
            try {
                if (o.items == null || o.items.isEmpty()) {
                    o.status = "REJECTED";
                    System.out.println("Order " + o.id + " rejected: no items");
                    continue;
                }

                double subtotal = 0;
                for (OrderItem it : o.items) {
                    subtotal += it.price * it.quantity;
                    subtotal = Math.round(subtotal * 100.0) / 100.0;
                }

                double tax = subtotal * 0.15;
                double serviceFee = (o.items.size() > 2) ? 2.0 : 1.0;

                double discount = 0;
                if (o.customer != null && o.customer.loyaltyPoints > 1000) {
                    discount = subtotal * 0.05;
                } else if (GLOBAL_DISCOUNT > 0) {
                    discount = subtotal * GLOBAL_DISCOUNT / 100.0;
                }

                if ("VIP".equals(o.customer != null ? o.customer.type : null)) {
                    double vipExtra = o.customer.getVipMultiplier() * 10;
                    subtotal -= vipExtra;
                }

                double total = subtotal + tax + serviceFee - discount;
                total = Math.round(total * 100.0) / 100.0;

                try {
                    persistOrder(o);
                } catch (Exception e) {
                }

                if (notifyCustomers && o.customer != null && o.customer.email != null) {
                    sendEmail(o.customer.email, buildReceiptText(o, total));
                }

                o.status = "PROCESSED";
                System.out.println("Order " + o.id + " processed: total=" + total);

            } catch (RuntimeException ex) {
                ex.printStackTrace();
            }
        }

        int processedCount = 0;
        for (Order o : inputOrders) {
            if ("PROCESSED".equals(o.status)) processedCount++;
        }

        String adminReport = "Processed " + processedCount + " orders on " + nowString();
        System.out.println(adminReport);
    }

    private void persistOrder(Order o) throws Exception {
        if (o.id % 7 == 0) {
            throw new RuntimeException("Simulated DB failure");
        }
    }

    private String buildReceiptText(Order o, double total) {
        String date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        StringBuilder sb = new StringBuilder();
        sb.append("Receipt for order ").append(o.id).append("\n");
        sb.append("Date: ").append(date).append("\n");
        sb.append("Customer: ").append(o.customer != null ? o.customer.name : "Guest").append("\n");
        sb.append("Total: ").append(total).append("\n");
        return sb.toString();
    }

    private void sendEmail(String to, String body) {
        System.out.println("Sending email to " + to);
        System.out.println(body);
    }

    private String nowString() {
        Calendar c = Calendar.getInstance();
        return String.format("%04d-%02d-%02d", c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH));
    }

    public void legacyExport(List<Order> orders, boolean includeDetails, boolean compress, int formatVersion) {
    }

    private void legacyProcess(Order o) {
        o.status = "LEGACY";
    }
}

class Order {
    public int id;
    public List<OrderItem> items;
    public Customer customer;
    public String status;
    public Map<String, String> meta = new HashMap<>();

    public Order(int id) {
        this.id = id;
        this.items = new ArrayList<>();
    }
}

class OrderItem {
    public String name;
    public double price;
    public int quantity;

    public OrderItem(String n, double p, int q) {
        this.name = n;
        this.price = p;
        this.quantity = q;
    }
}

class Customer {
    public String name;
    public String email;
    public String type;
    public int loyaltyPoints;

    public Customer(String name, String email) {
        this.name = name;
        this.email = email;
    }

    public double getVipMultiplier() {
        return 1.5;
    }
}
