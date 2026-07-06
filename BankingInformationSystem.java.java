import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Scanner;

/* =====================================================================
 * BANKING INFORMATION SYSTEM - CORE JAVA PROTOTYPE (SINGLE FILE VERSION)
 * ---------------------------------------------------------------------
 * This file merges every class from the original multi-file project
 * into one compilation unit for convenience:
 *   - Custom exceptions (error handling)
 *   - Model classes: Transaction, Account, User
 *   - DataStore (persistence to disk)
 *   - Bank (core business logic)
 *   - BankingApp (console UI + main method)
 *
 * Compile:  javac BankingInformationSystem.java
 * Run:      java BankingApp
 * ===================================================================== */


// =========================================================================
// CUSTOM EXCEPTIONS (Error Handling requirement)
// =========================================================================

/** Thrown when a withdrawal or transfer amount exceeds the available balance. */
class InsufficientFundsException extends Exception {
    public InsufficientFundsException(String message) {
        super(message);
    }
}

/** Thrown when a transaction fails basic validation (e.g. negative/zero amount). */
class InvalidTransactionException extends Exception {
    public InvalidTransactionException(String message) {
        super(message);
    }
}

/** Thrown when an operation references an account number that does not exist. */
class AccountNotFoundException extends Exception {
    public AccountNotFoundException(String message) {
        super(message);
    }
}

/** Thrown when a login attempt fails due to a wrong username/password. */
class AuthenticationException extends Exception {
    public AuthenticationException(String message) {
        super(message);
    }
}


// =========================================================================
// MODEL: Transaction
// =========================================================================

/** Represents a single ledger entry against an account. */
class Transaction implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");

    public enum Type { DEPOSIT, WITHDRAWAL, TRANSFER_OUT, TRANSFER_IN }

    private final Type type;
    private final double amount;
    private final double resultingBalance;
    private final LocalDateTime timestamp;
    private final String description;

    public Transaction(Type type, double amount, double resultingBalance, String description) {
        this.type = type;
        this.amount = amount;
        this.resultingBalance = resultingBalance;
        this.timestamp = LocalDateTime.now();
        this.description = description;
    }

    public Type getType() {
        return type;
    }

    public double getAmount() {
        return amount;
    }

    public double getResultingBalance() {
        return resultingBalance;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public String getDescription() {
        return description;
    }

    /** Formats this transaction as a single line for the account statement view. */
    public String toStatementLine() {
        return String.format("%-19s | %-13s | %10.2f | %10.2f | %s",
                timestamp.format(FORMATTER), type, amount, resultingBalance, description);
    }

    @Override
    public String toString() {
        return toStatementLine();
    }
}


// =========================================================================
// MODEL: Account
// =========================================================================

/** Represents a single bank account belonging to a user. */
class Account implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String accountNumber;
    private final String ownerUsername;
    private double balance;
    private final List<Transaction> history = new ArrayList<>();

    public Account(String accountNumber, String ownerUsername, double openingBalance) {
        this.accountNumber = accountNumber;
        this.ownerUsername = ownerUsername;
        this.balance = openingBalance;
        if (openingBalance > 0) {
            history.add(new Transaction(Transaction.Type.DEPOSIT, openingBalance,
                    balance, "Initial deposit at account opening"));
        }
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public String getOwnerUsername() {
        return ownerUsername;
    }

    public double getBalance() {
        return balance;
    }

    public List<Transaction> getHistory() {
        return Collections.unmodifiableList(history);
    }

    /** Adds funds to the account. */
    public void deposit(double amount) throws InvalidTransactionException {
        if (amount <= 0) {
            throw new InvalidTransactionException("Deposit amount must be greater than zero.");
        }
        balance += amount;
        history.add(new Transaction(Transaction.Type.DEPOSIT, amount, balance, "Cash deposit"));
    }

    /** Removes funds from the account, checking for sufficient balance. */
    public void withdraw(double amount) throws InvalidTransactionException, InsufficientFundsException {
        if (amount <= 0) {
            throw new InvalidTransactionException("Withdrawal amount must be greater than zero.");
        }
        if (amount > balance) {
            throw new InsufficientFundsException(
                    String.format("Insufficient funds: balance is %.2f but %.2f was requested.", balance, amount));
        }
        balance -= amount;
        history.add(new Transaction(Transaction.Type.WITHDRAWAL, amount, balance, "Cash withdrawal"));
    }

    /** Debits this account as the sending side of a fund transfer. */
    public void transferOut(double amount, String toAccountNumber)
            throws InvalidTransactionException, InsufficientFundsException {
        if (amount <= 0) {
            throw new InvalidTransactionException("Transfer amount must be greater than zero.");
        }
        if (amount > balance) {
            throw new InsufficientFundsException(
                    String.format("Insufficient funds: balance is %.2f but %.2f was requested.", balance, amount));
        }
        balance -= amount;
        history.add(new Transaction(Transaction.Type.TRANSFER_OUT, amount, balance,
                "Transfer to account " + toAccountNumber));
    }

    /** Credits this account as the receiving side of a fund transfer. */
    public void transferIn(double amount, String fromAccountNumber) {
        balance += amount;
        history.add(new Transaction(Transaction.Type.TRANSFER_IN, amount, balance,
                "Transfer from account " + fromAccountNumber));
    }
}


// =========================================================================
// MODEL: User
// =========================================================================

/** Represents a registered user of the banking system. */
class User implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String username;
    private int passwordHash;
    private String fullName;
    private String address;
    private String contactNumber;
    private final List<String> accountNumbers = new ArrayList<>();

    public User(String username, String password, String fullName, String address, String contactNumber) {
        this.username = username;
        this.passwordHash = Objects.hashCode(password);
        this.fullName = fullName;
        this.address = address;
        this.contactNumber = contactNumber;
    }

    public String getUsername() {
        return username;
    }

    public boolean checkPassword(String password) {
        return this.passwordHash == Objects.hashCode(password);
    }

    public void setPassword(String newPassword) {
        this.passwordHash = Objects.hashCode(newPassword);
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getContactNumber() {
        return contactNumber;
    }

    public void setContactNumber(String contactNumber) {
        this.contactNumber = contactNumber;
    }

    public List<String> getAccountNumbers() {
        return accountNumbers;
    }

    public void addAccountNumber(String accountNumber) {
        accountNumbers.add(accountNumber);
    }

    @Override
    public String toString() {
        return String.format("Username: %s | Name: %s | Address: %s | Contact: %s | Accounts: %s",
                username, fullName, address, contactNumber, accountNumbers);
    }
}


// =========================================================================
// PERSISTENCE: DataStore
// =========================================================================

/**
 * Handles saving and loading the bank's in-memory state (users and
 * accounts) to/from a single serialized file on disk, so that data
 * created in one session is still available the next time the
 * prototype is run.
 */
class DataStore {
    private static final String DATA_FILE = "bank_data.ser";

    /** Simple container so both maps can be written/read in one shot. */
    private static class Snapshot implements Serializable {
        private static final long serialVersionUID = 1L;
        Map<String, User> users;
        Map<String, Account> accounts;
        int nextAccountNumberSeed;
    }

    public static void save(Map<String, User> users, Map<String, Account> accounts, int nextAccountNumberSeed) {
        Snapshot snapshot = new Snapshot();
        snapshot.users = users;
        snapshot.accounts = accounts;
        snapshot.nextAccountNumberSeed = nextAccountNumberSeed;

        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(DATA_FILE))) {
            out.writeObject(snapshot);
        } catch (IOException e) {
            System.out.println("Warning: could not save data to disk (" + e.getMessage() + ")");
        }
    }

    /**
     * Loads previously saved data. Returns an empty, freshly
     * initialized snapshot if no data file exists yet (first run)
     * or if the file cannot be read.
     */
    @SuppressWarnings("unchecked")
    public static LoadResult load() {
        File file = new File(DATA_FILE);
        if (!file.exists()) {
            return new LoadResult(new HashMap<>(), new HashMap<>(), 1001);
        }

        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(file))) {
            Snapshot snapshot = (Snapshot) in.readObject();
            return new LoadResult(snapshot.users, snapshot.accounts, snapshot.nextAccountNumberSeed);
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("Warning: could not load existing data, starting fresh (" + e.getMessage() + ")");
            return new LoadResult(new HashMap<>(), new HashMap<>(), 1001);
        }
    }

    /** Small result holder returned by load(). */
    public static class LoadResult {
        public final Map<String, User> users;
        public final Map<String, Account> accounts;
        public final int nextAccountNumberSeed;

        public LoadResult(Map<String, User> users, Map<String, Account> accounts, int nextAccountNumberSeed) {
            this.users = users;
            this.accounts = accounts;
            this.nextAccountNumberSeed = nextAccountNumberSeed;
        }
    }
}


// =========================================================================
// BANK: core business logic
// =========================================================================

/**
 * Central service class that owns all users and accounts and
 * implements every core banking operation: registration, account
 * management, deposits, withdrawals, transfers, statements, and
 * login authentication.
 */
class Bank {
    private final Map<String, User> usersByUsername;
    private final Map<String, Account> accountsByNumber;
    private int nextAccountNumberSeed;

    public Bank() {
        DataStore.LoadResult loaded = DataStore.load();
        this.usersByUsername = loaded.users;
        this.accountsByNumber = loaded.accounts;
        this.nextAccountNumberSeed = loaded.nextAccountNumberSeed;
    }

    /** Persists the current in-memory state to disk. */
    public void save() {
        DataStore.save(usersByUsername, accountsByNumber, nextAccountNumberSeed);
    }

    // ------------------------------------------------------------------
    // 1. User Registration
    // ------------------------------------------------------------------

    /**
     * Registers a new user and opens their first account in one step.
     *
     * @return the newly created Account (its account number is the
     *         confirmation the caller should show the user).
     */
    public Account registerUser(String username, String password, String fullName,
                                 String address, String contactNumber, double initialDeposit)
            throws InvalidTransactionException {
        if (usersByUsername.containsKey(username)) {
            throw new InvalidTransactionException("Username '" + username + "' is already taken.");
        }
        if (username == null || username.trim().isEmpty()) {
            throw new InvalidTransactionException("Username cannot be empty.");
        }
        if (password == null || password.length() < 4) {
            throw new InvalidTransactionException("Password must be at least 4 characters long.");
        }
        if (initialDeposit < 0) {
            throw new InvalidTransactionException("Initial deposit cannot be negative.");
        }

        User user = new User(username, password, fullName, address, contactNumber);
        Account account = openAccountFor(user, initialDeposit);
        usersByUsername.put(username, user);
        return account;
    }

    /** Opens an additional account for an already-registered user. */
    public Account openAccountFor(User user, double openingBalance) {
        String accountNumber = generateAccountNumber();
        Account account = new Account(accountNumber, user.getUsername(), openingBalance);
        accountsByNumber.put(accountNumber, account);
        user.addAccountNumber(accountNumber);
        return account;
    }

    private String generateAccountNumber() {
        String number = "ACC" + nextAccountNumberSeed;
        nextAccountNumberSeed++;
        return number;
    }

    // ------------------------------------------------------------------
    // Authentication
    // ------------------------------------------------------------------

    public User login(String username, String password) throws AuthenticationException {
        User user = usersByUsername.get(username);
        if (user == null || !user.checkPassword(password)) {
            throw new AuthenticationException("Invalid username or password.");
        }
        return user;
    }

    // ------------------------------------------------------------------
    // 2. Account Management
    // ------------------------------------------------------------------

    public void updateUserDetails(User user, String newFullName, String newAddress, String newContact) {
        if (newFullName != null && !newFullName.trim().isEmpty()) {
            user.setFullName(newFullName);
        }
        if (newAddress != null && !newAddress.trim().isEmpty()) {
            user.setAddress(newAddress);
        }
        if (newContact != null && !newContact.trim().isEmpty()) {
            user.setContactNumber(newContact);
        }
    }

    public void changePassword(User user, String newPassword) throws InvalidTransactionException {
        if (newPassword == null || newPassword.length() < 4) {
            throw new InvalidTransactionException("Password must be at least 4 characters long.");
        }
        user.setPassword(newPassword);
    }

    public Account getAccount(String accountNumber) throws AccountNotFoundException {
        Account account = accountsByNumber.get(accountNumber);
        if (account == null) {
            throw new AccountNotFoundException("No account found with number " + accountNumber);
        }
        return account;
    }

    /** Verifies the account belongs to the given user, for authorization checks. */
    public void assertOwnership(User user, Account account) throws AccountNotFoundException {
        if (!account.getOwnerUsername().equals(user.getUsername())) {
            throw new AccountNotFoundException("Account " + account.getAccountNumber() +
                    " does not belong to the logged-in user.");
        }
    }

    // ------------------------------------------------------------------
    // 3. Deposit and Withdrawal
    // ------------------------------------------------------------------

    public void deposit(Account account, double amount) throws InvalidTransactionException {
        account.deposit(amount);
    }

    public void withdraw(Account account, double amount)
            throws InvalidTransactionException, InsufficientFundsException {
        account.withdraw(amount);
    }

    // ------------------------------------------------------------------
    // 4. Fund Transfer
    // ------------------------------------------------------------------

    public void transfer(Account from, String toAccountNumber, double amount)
            throws InvalidTransactionException, InsufficientFundsException, AccountNotFoundException {
        if (from.getAccountNumber().equals(toAccountNumber)) {
            throw new InvalidTransactionException("Cannot transfer to the same account.");
        }
        Account to = getAccount(toAccountNumber);
        from.transferOut(amount, toAccountNumber);
        to.transferIn(amount, from.getAccountNumber());
    }

    // ------------------------------------------------------------------
    // Lookups used by the UI layer
    // ------------------------------------------------------------------

    public Map<String, Account> getAccountsByNumber() {
        return accountsByNumber;
    }

    public Map<String, User> getUsersByUsername() {
        return usersByUsername;
    }
}


// =========================================================================
// UI: BankingApp (contains the main method - PUBLIC class must match filename)
// =========================================================================

/**
 * Console-based user interface for the Banking Information System
 * prototype. Presents a menu-driven flow: register / login, then
 * (once logged in) deposit, withdraw, transfer, view statements,
 * and manage account details.
 */
public class BankingApp {
    private final Bank bank = new Bank();
    private final Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) {
        new BankingApp().run();
    }

    public void run() {
        printBanner();
        boolean running = true;
        while (running) {
            printMainMenu();
            String choice = scanner.nextLine().trim();
            switch (choice) {
                case "1":
                    handleRegistration();
                    break;
                case "2":
                    handleLogin();
                    break;
                case "3":
                    running = false;
                    break;
                default:
                    System.out.println("Invalid option. Please choose 1, 2, or 3.");
            }
        }
        bank.save();
        System.out.println("\nThank you for using the Banking Information System. Goodbye!");
    }

    // ------------------------------------------------------------------
    // Banners / menus
    // ------------------------------------------------------------------

    private void printBanner() {
        System.out.println("=======================================================");
        System.out.println("        BANKING INFORMATION SYSTEM - PROTOTYPE");
        System.out.println("=======================================================");
    }

    private void printMainMenu() {
        System.out.println("\n--------------- MAIN MENU ---------------");
        System.out.println("1. Register New User");
        System.out.println("2. Login");
        System.out.println("3. Exit");
        System.out.print("Enter your choice: ");
    }

    private void printAccountMenu(User user) {
        System.out.println("\n----------- ACCOUNT MENU (" + user.getUsername() + ") -----------");
        System.out.println("1. Deposit");
        System.out.println("2. Withdraw");
        System.out.println("3. Transfer Funds");
        System.out.println("4. View Account Statement");
        System.out.println("5. View / Update Account Details");
        System.out.println("6. Open Another Account");
        System.out.println("7. Change Password");
        System.out.println("8. Logout");
        System.out.print("Enter your choice: ");
    }

    // ------------------------------------------------------------------
    // 1. User Registration flow
    // ------------------------------------------------------------------

    private void handleRegistration() {
        System.out.println("\n--- New User Registration ---");
        System.out.print("Choose a username: ");
        String username = scanner.nextLine().trim();
        System.out.print("Choose a password (min 4 characters): ");
        String password = scanner.nextLine().trim();
        System.out.print("Full name: ");
        String fullName = scanner.nextLine().trim();
        System.out.print("Address: ");
        String address = scanner.nextLine().trim();
        System.out.print("Contact number: ");
        String contact = scanner.nextLine().trim();
        double initialDeposit = readDouble("Initial deposit amount: ");

        try {
            Account account = bank.registerUser(username, password, fullName, address, contact, initialDeposit);
            bank.save();
            System.out.println("\n*** Registration Successful! ***");
            System.out.println("Your new account number is: " + account.getAccountNumber());
            System.out.printf("Opening balance: %.2f%n", account.getBalance());
        } catch (InvalidTransactionException e) {
            System.out.println("Registration failed: " + e.getMessage());
        }
    }

    // ------------------------------------------------------------------
    // Login flow
    // ------------------------------------------------------------------

    private void handleLogin() {
        System.out.println("\n--- Login ---");
        System.out.print("Username: ");
        String username = scanner.nextLine().trim();
        System.out.print("Password: ");
        String password = scanner.nextLine().trim();

        try {
            User user = bank.login(username, password);
            System.out.println("\nLogin successful. Welcome, " + user.getFullName() + "!");
            sessionLoop(user);
        } catch (AuthenticationException e) {
            System.out.println("Login failed: " + e.getMessage());
        }
    }

    private void sessionLoop(User user) {
        boolean loggedIn = true;
        while (loggedIn) {
            printAccountMenu(user);
            String choice = scanner.nextLine().trim();
            switch (choice) {
                case "1":
                    handleDeposit(user);
                    break;
                case "2":
                    handleWithdraw(user);
                    break;
                case "3":
                    handleTransfer(user);
                    break;
                case "4":
                    handleStatement(user);
                    break;
                case "5":
                    handleAccountDetails(user);
                    break;
                case "6":
                    handleOpenAnotherAccount(user);
                    break;
                case "7":
                    handleChangePassword(user);
                    break;
                case "8":
                    loggedIn = false;
                    bank.save();
                    System.out.println("Logged out successfully.");
                    break;
                default:
                    System.out.println("Invalid option. Please choose between 1 and 8.");
            }
        }
    }

    // ------------------------------------------------------------------
    // Helper: resolve which of the logged-in user's accounts to operate on
    // ------------------------------------------------------------------

    private Account selectOwnAccount(User user) throws AccountNotFoundException {
        if (user.getAccountNumbers().size() == 1) {
            return bank.getAccount(user.getAccountNumbers().get(0));
        }
        System.out.println("Your accounts: " + user.getAccountNumbers());
        System.out.print("Enter account number to use: ");
        String accNum = scanner.nextLine().trim();
        Account account = bank.getAccount(accNum);
        bank.assertOwnership(user, account);
        return account;
    }

    // ------------------------------------------------------------------
    // 3. Deposit and Withdrawal
    // ------------------------------------------------------------------

    private void handleDeposit(User user) {
        try {
            Account account = selectOwnAccount(user);
            double amount = readDouble("Enter deposit amount: ");
            bank.deposit(account, amount);
            bank.save();
            System.out.printf("*** Deposit successful. New balance: %.2f ***%n", account.getBalance());
        } catch (AccountNotFoundException | InvalidTransactionException e) {
            System.out.println("Deposit failed: " + e.getMessage());
        }
    }

    private void handleWithdraw(User user) {
        try {
            Account account = selectOwnAccount(user);
            double amount = readDouble("Enter withdrawal amount: ");
            bank.withdraw(account, amount);
            bank.save();
            System.out.printf("*** Withdrawal successful. New balance: %.2f ***%n", account.getBalance());
        } catch (AccountNotFoundException | InvalidTransactionException | InsufficientFundsException e) {
            System.out.println("Withdrawal failed: " + e.getMessage());
        }
    }

    // ------------------------------------------------------------------
    // 4. Fund Transfer
    // ------------------------------------------------------------------

    private void handleTransfer(User user) {
        try {
            Account from = selectOwnAccount(user);
            System.out.print("Enter recipient's account number: ");
            String toAccountNumber = scanner.nextLine().trim();
            double amount = readDouble("Enter amount to transfer: ");
            bank.transfer(from, toAccountNumber, amount);
            bank.save();
            Account to = bank.getAccount(toAccountNumber);
            System.out.println("*** Transfer successful! ***");
            System.out.printf("Your new balance (%s): %.2f%n", from.getAccountNumber(), from.getBalance());
            System.out.printf("Recipient balance (%s): %.2f%n", to.getAccountNumber(), to.getBalance());
        } catch (AccountNotFoundException | InvalidTransactionException | InsufficientFundsException e) {
            System.out.println("Transfer failed: " + e.getMessage());
        }
    }

    // ------------------------------------------------------------------
    // 5. Account Statements
    // ------------------------------------------------------------------

    private void handleStatement(User user) {
        try {
            Account account = selectOwnAccount(user);
            System.out.println("\n--- Statement for Account " + account.getAccountNumber() + " ---");
            System.out.printf("%-19s | %-13s | %10s | %10s | %s%n",
                    "Date/Time", "Type", "Amount", "Balance", "Description");
            System.out.println("-".repeat(90));
            if (account.getHistory().isEmpty()) {
                System.out.println("No transactions yet.");
            } else {
                for (Transaction t : account.getHistory()) {
                    System.out.println(t.toStatementLine());
                }
            }
            System.out.printf("%nCurrent Balance: %.2f%n", account.getBalance());
        } catch (AccountNotFoundException e) {
            System.out.println("Could not display statement: " + e.getMessage());
        }
    }

    // ------------------------------------------------------------------
    // 2. Account Management (view / update details)
    // ------------------------------------------------------------------

    private void handleAccountDetails(User user) {
        System.out.println("\n--- Your Account Details ---");
        System.out.println(user);
        System.out.print("\nUpdate details? (y/n): ");
        String resp = scanner.nextLine().trim();
        if (!resp.equalsIgnoreCase("y")) {
            return;
        }
        System.out.print("New full name (leave blank to keep current): ");
        String fullName = scanner.nextLine().trim();
        System.out.print("New address (leave blank to keep current): ");
        String address = scanner.nextLine().trim();
        System.out.print("New contact number (leave blank to keep current): ");
        String contact = scanner.nextLine().trim();

        bank.updateUserDetails(user, fullName, address, contact);
        bank.save();
        System.out.println("*** Account information updated successfully. ***");
    }

    private void handleOpenAnotherAccount(User user) {
        double openingBalance = readDouble("Opening balance for new account: ");
        if (openingBalance < 0) {
            System.out.println("Opening balance cannot be negative.");
            return;
        }
        Account account = bank.openAccountFor(user, openingBalance);
        bank.save();
        System.out.println("*** New account created: " + account.getAccountNumber() + " ***");
    }

    private void handleChangePassword(User user) {
        System.out.print("Enter new password (min 4 characters): ");
        String newPassword = scanner.nextLine().trim();
        try {
            bank.changePassword(user, newPassword);
            bank.save();
            System.out.println("*** Password changed successfully. ***");
        } catch (InvalidTransactionException e) {
            System.out.println("Could not change password: " + e.getMessage());
        }
    }

    // ------------------------------------------------------------------
    // Input helper with basic error handling for invalid numbers
    // ------------------------------------------------------------------

    private double readDouble(String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim();
            try {
                return Double.parseDouble(input);
            } catch (NumberFormatException e) {
                System.out.println("Please enter a valid numeric amount.");
            }
        }
    }
}
