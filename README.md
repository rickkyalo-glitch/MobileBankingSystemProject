# Wire — Mobile Money (JavaFX)

A JavaFX + SQLite desktop banking simulator: register, log in, deposit,
withdraw, send money, pay a till, save, view a statement, and an admin panel
for income/user/transaction reports.

## Running it

You need JDK 21+ and Maven installed.

```
mvn javafx:run
```

That's it — Maven downloads JavaFX and the SQLite driver itself. `wire.db`
is created automatically in the project root on first run, with a default
admin account (`admin` / `admin123` — change this after logging in once).

To build a runnable jar instead: `mvn package`.

## What changed from the original

**It wouldn't build on another machine.** The old project had no `pom.xml` —
`.vscode/settings.json` pointed at `C:\Users\mutho\Downloads\javafx-sdk-25.0.1\...`,
a path that only exists on one laptop. This version is a standard Maven
project (`org.openjfx:javafx-controls`/`javafx-fxml` + `org.xerial:sqlite-jdbc`
as declared dependencies), so `mvn javafx:run` works anywhere Maven and a JDK
are installed. I wasn't able to actually run `mvn compile` in the sandbox I
wrote this in (no Maven Central access there), so run it once on your machine
and let me know if anything doesn't resolve — the dependency versions
(`javafx 21.0.4`, `sqlite-jdbc 3.53.4.0`) were current as of writing but worth
double-checking if it's been a while.

**Send Money silently deleted money.** `processSendMoney` deducted the
sender's balance and logged a transaction row, but never looked up the
recipient or credited their account — the money just vanished. It now looks
the recipient up by phone number first (and refuses before touching any
balance if no account exists), and credits them for the transfer amount.

**Passwords weren't actually protected.** The admin password was stored as
plain text (`admin123` sitting in the `admins` table), and PINs were hashed
with plain unsalted SHA-256 — fast to attack, and a 4-digit PIN only has
10,000 possible values anyway. Both now go through `PasswordUtil`, which
uses salted PBKDF2 with a high iteration count (120,000), the standard way
to slow down offline guessing.

**A real bug in the default-admin setup.** `initializeDatabase()` called
`rs.getInt(1)` without first calling `rs.next()` — reading a `ResultSet`
before advancing its cursor is undefined behavior per the JDBC spec. Fixed.

**Resource leaks.** Several `Statement`/`ResultSet` objects (e.g. in the
admin reports and `getLastTransactionId`) were never closed. Everything now
uses try-with-resources.

**Missing image asset.** Every screen's FXML referenced
`images/background.jpg`, which was never included in the project — so it
would have loaded with a broken image at runtime on any machine. Replaced
with a CSS gradient background (`styles.css`) that needs no external file.

**Duplicated styling code.** All five controllers had an identical
`setupButtonStyles()` method that inlined the same CSS strings and hover
listeners. Moved into one shared `styles.css` stylesheet referenced by every
FXML file — one place to change the look, no Java code needed for styling.

**Duplicated navigation/alert code.** Every controller repeated the same
`FXMLLoader` + `Stage` scene-swap and the same 4-line `Alert` builder.
Extracted into `wire.util.SceneNavigator`.

**Swallowed errors.** The original used `System.out.println`/`e.printStackTrace()`
throughout and showed the same generic "Database Error" message regardless
of what went wrong, so failures were invisible except in a console the user
never sees. Replaced with `java.util.logging` and error handling that fails
loudly (startup errors now show an Alert instead of a blank window).

**Inconsistent input validation.** Phone numbers weren't validated
consistently (registration accepted anything; Send Money only checked
length; login didn't check at all). Now a single Kenyan phone pattern
(`07XXXXXXXX` / `01XXXXXXXX` / `+254...`) is applied everywhere a phone
number is entered. Also added: can't send money to your own number, PIN
must be exactly 4 digits (previously only checked length, not that it was
numeric).

## What I didn't change

The overall design — SQLite file DB, PIN-based auth, ID-number identity
check for changing your PIN, the fee model (0.1% over KSh 500) — is
unchanged. Those are product decisions, not bugs, and the brief was to fix
and polish rather than redesign.

## Project structure

```
pom.xml
src/main/java/wire/
    Main.java
    User.java
    DatabaseConnection.java
    LoginController.java
    RegisterController.java
    AdminLoginController.java
    AdminDashboardController.java
    DashboardController.java
    util/
        PasswordUtil.java
        SceneNavigator.java
src/main/resources/wire/fxml/
    login.fxml
    register.fxml
    admin_login.fxml
    admin_dashboard.fxml
    dashboard.fxml
    styles.css
```
