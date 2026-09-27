package banking;

import java.sql.*;
import java.util.Scanner;

public class BankingManagement {
    static final String URL = "jdbc:mysql://localhost:3306/banking_management";
    static final String DB_USER = "root";
    static final String DB_PASSWORD = "14367898Lm&";

    static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, DB_USER, DB_PASSWORD);
    }

    static void createCustomer(Scanner sc) {
        String sql = "INSERT INTO customers(name,email,phone) VALUES(?,?,?)";
        try (Connection con = getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            System.out.print("Name: "); ps.setString(1, sc.nextLine());
            System.out.print("Email: "); ps.setString(2, sc.nextLine());
            System.out.print("Phone: "); ps.setString(3, sc.nextLine());
            ps.executeUpdate();
            System.out.println("Customer created.");
        } catch (SQLException e) { System.out.println("Error: " + e.getMessage()); }
    }

    static void createAccount(Scanner sc) {
        String sql = "INSERT INTO accounts(customer_id,account_number,balance) VALUES(?,?,0)";
        try (Connection con = getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            System.out.print("Customer ID: "); ps.setInt(1, Integer.parseInt(sc.nextLine()));
            System.out.print("Account number: "); ps.setString(2, sc.nextLine());
            ps.executeUpdate();
            System.out.println("Account created.");
        } catch (SQLException e) { System.out.println("Error: " + e.getMessage()); }
    }

    static void deposit(Scanner sc) {
        Connection con = null;
        try {
            con = getConnection();
            con.setAutoCommit(false);

            System.out.print("Account ID: ");
            int id = Integer.parseInt(sc.nextLine());
            System.out.print("Amount: ");
            double amount = Double.parseDouble(sc.nextLine());

            if (amount <= 0) throw new IllegalArgumentException("Amount must be positive.");

            try (PreparedStatement p1 = con.prepareStatement(
                    "UPDATE accounts SET balance=balance+? WHERE account_id=?");
                 PreparedStatement p2 = con.prepareStatement(
                    "INSERT INTO transactions(account_id,transaction_type,amount) VALUES(?,?,?)")) {
                p1.setDouble(1, amount); p1.setInt(2, id);
                if (p1.executeUpdate() != 1) throw new SQLException("Account not found.");

                p2.setInt(1, id); p2.setString(2, "DEPOSIT"); p2.setDouble(3, amount);
                p2.executeUpdate();
                con.commit();
                System.out.println("Deposit successful.");
            }
        } catch (Exception e) {
            try { if (con != null) con.rollback(); } catch (SQLException ignored) {}
            System.out.println("Deposit failed: " + e.getMessage());
        } finally {
            try { if (con != null) con.close(); } catch (SQLException ignored) {}
        }
    }

    static void withdraw(Scanner sc) {
        Connection con = null;
        try {
            con = getConnection();
            con.setAutoCommit(false);

            System.out.print("Account ID: ");
            int id = Integer.parseInt(sc.nextLine());
            System.out.print("Amount: ");
            double amount = Double.parseDouble(sc.nextLine());

            if (amount <= 0) throw new IllegalArgumentException("Amount must be positive.");

            try (PreparedStatement p1 = con.prepareStatement(
                    "UPDATE accounts SET balance=balance-? WHERE account_id=? AND balance>=?");
                 PreparedStatement p2 = con.prepareStatement(
                    "INSERT INTO transactions(account_id,transaction_type,amount) VALUES(?,?,?)")) {
                p1.setDouble(1, amount); p1.setInt(2, id); p1.setDouble(3, amount);
                if (p1.executeUpdate() != 1) throw new SQLException("Insufficient balance or invalid account.");

                p2.setInt(1, id); p2.setString(2, "WITHDRAW"); p2.setDouble(3, amount);
                p2.executeUpdate();
                con.commit();
                System.out.println("Withdrawal successful.");
            }
        } catch (Exception e) {
            try { if (con != null) con.rollback(); } catch (SQLException ignored) {}
            System.out.println("Withdrawal failed: " + e.getMessage());
        } finally {
            try { if (con != null) con.close(); } catch (SQLException ignored) {}
        }
    }

    static void transfer(Scanner sc) {
        Connection con = null;
        try {
            con = getConnection();
            con.setAutoCommit(false);

            System.out.print("From Account ID: ");
            int from = Integer.parseInt(sc.nextLine());
            System.out.print("To Account ID: ");
            int to = Integer.parseInt(sc.nextLine());
            System.out.print("Amount: ");
            double amount = Double.parseDouble(sc.nextLine());

            if (from == to || amount <= 0)
                throw new IllegalArgumentException("Invalid transfer.");

            try (PreparedStatement debit = con.prepareStatement(
                    "UPDATE accounts SET balance=balance-? WHERE account_id=? AND balance>=?");
                 PreparedStatement credit = con.prepareStatement(
                    "UPDATE accounts SET balance=balance+? WHERE account_id=?");
                 PreparedStatement log = con.prepareStatement(
                    "INSERT INTO transactions(account_id,transaction_type,amount) VALUES(?,?,?)")) {

                debit.setDouble(1, amount); debit.setInt(2, from); debit.setDouble(3, amount);
                if (debit.executeUpdate() != 1)
                    throw new SQLException("Invalid source account or insufficient balance.");

                credit.setDouble(1, amount); credit.setInt(2, to);
                if (credit.executeUpdate() != 1)
                    throw new SQLException("Destination account not found.");

                log.setInt(1, from); log.setString(2, "TRANSFER_OUT"); log.setDouble(3, amount);
                log.executeUpdate();
                log.setInt(1, to); log.setString(2, "TRANSFER_IN"); log.setDouble(3, amount);
                log.executeUpdate();

                con.commit();
                System.out.println("Transfer successful.");
            }
        } catch (Exception e) {
            try { if (con != null) con.rollback(); } catch (SQLException ignored) {}
            System.out.println("Transfer failed. No money was deducted: " + e.getMessage());
        } finally {
            try { if (con != null) con.close(); } catch (SQLException ignored) {}
        }
    }

    static void showTransactions(Scanner sc) {
        String sql = "SELECT transaction_id,transaction_type,amount,transaction_date " +
                     "FROM transactions WHERE account_id=? ORDER BY transaction_date DESC";
        try (Connection con = getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            System.out.print("Account ID: ");
            ps.setInt(1, Integer.parseInt(sc.nextLine()));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    System.out.printf("%d | %s | %.2f | %s%n",
                        rs.getInt(1), rs.getString(2), rs.getDouble(3), rs.getTimestamp(4));
                }
            }
        } catch (SQLException e) { System.out.println("Error: " + e.getMessage()); }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        while (true) {
            System.out.println("\n--- BANKING MANAGEMENT ---");
            System.out.println("1. Create Customer");
            System.out.println("2. Create Account");
            System.out.println("3. Deposit");
            System.out.println("4. Withdraw");
            System.out.println("5. Transfer Funds");
            System.out.println("6. Transaction History");
            System.out.println("0. Exit");
            System.out.print("Choice: ");

            switch (sc.nextLine()) {
                case "1" -> createCustomer(sc);
                case "2" -> createAccount(sc);
                case "3" -> deposit(sc);
                case "4" -> withdraw(sc);
                case "5" -> transfer(sc);
                case "6" -> showTransactions(sc);
                case "0" -> { sc.close(); return; }
                default -> System.out.println("Invalid choice.");
            }
        }
    }
}

		// TODO Auto-generated method stub
