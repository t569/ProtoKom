# ProtoKom

> A bare‑bones, thread‑per‑connection Java server–client playground.

---

## 🚀 Quick & Dirty Overview

Imagine you’ve got a rag‑tag bunch of Java classes that stitch together to give you a minimal server–client playground. On the **server** side you’ve got:

- **DataProviders** and **DataBindings** to abstract where your data lives (could be a real DB, could be your imagination).
- A simple **Dummy** POJO for play‑data.
- A **Protocol** definition to keep messages consistent.
- **ProtoServer** that listens for connections and dispatches incoming requests.
- **ProtoClient** for testing against the server.
- **Main** to kick everything off.

Meanwhile, tucked in **routines/** is a cute little `Echo` class that shows off how you can plug in new “jobs” or “workers” without touching the server core.

---

## 📦 What’s in the `server/` Folder

### DataProvider & DataBindings

Think of `DataProvider<T>` as the interface that defines `get`, `post`, `update`, and `delete` methods for your data type T. Swap in any storage engine you want (in‑memory, SQL, NoSQL)—just implement this interface.

```java
public interface DataProvider<T> {
  T get(Object id)        throws Exception;
  void post(T entity)     throws Exception;
  void update(T entity)   throws Exception;
  void delete(Object id)  throws Exception;
}
```

`DataBindings<T>` is a safety net that makes sure you actually *bind* a provider before using it—otherwise you’ll hit an `IllegalStateException`.

```java
public class DataBindings<T> {
  private DataProvider<T> provider;

  public void bindToDataBase(DataProvider<T> p) {
    provider = p;
  }

  private void ensureProvider() {
    if (provider == null)
      throw new IllegalStateException("Bind a DataProvider before using!");
  }

  public T get(Object id) throws Exception {
    ensureProvider();
    return provider.get(id);
  }

  public void post(T entity) throws Exception {
    ensureProvider();
    provider.post(entity);
  }

  public void update(T entity) throws Exception {
    ensureProvider();
    provider.update(entity);
  }

  public void delete(Object id) throws Exception {
    ensureProvider();
    provider.delete(id);
  }
}
```

---

### Dummy Data for Fun & Profit

Need a simple object to play with? Enter **`Dummy.java`**—just an `id` and a `name`:

```java
public class Dummy {
  public int id;
  public String name;

  public Dummy(int id, String name) {
    this.id = id;
    this.name = name;
  }
}
```

---

### Protocol: The Contract

In **`Protocol.java`** you define the shapes of your request/response objects (enums, message classes, etc.). This is where both server and client agree on the “language” you’re speaking—no more magic strings.

---

### ProtoServer & Friends

- **`ProtoServer.java`**
  Listens on a TCP port, spawns a new thread per connection, reads incoming messages, and routes them to your data bindings or routines.

- **`ProtoClient.java`**
  A simple socket‑based client to send `Protocol` messages to your server and print the replies.

- **`Main.java`**
  Your entry point. Boots the server, binds a sample `Dummy` provider, maybe spins up a demo client, and ties all the pieces together.

---

## 🔄 The `routines/` Folder

### Echo Routine

Want to add a new routine? Check out **`routines/Echo.java`**:

```java
package routines;

public class Echo {
  private static final String DEFAULT_NAME = "Echo";
  private String name;

  public Echo(String name) {
    this.name = name;
  }

  public Echo() {
    this.name = DEFAULT_NAME;
  }

  public String echo(String message) {
    String formatted = "[ " + name + " ]: " + message;
    log(message);
    return formatted;
  }

  public void log(String message) {
    System.out.println("[ " + name + " ]: " + message);
  }

  public String log_err_with_ret(Exception e) {
    String formatted = "[ " + name + " ]: " + e.getMessage();
    System.err.println(formatted);
    return formatted;
  }
}
```

Drop your own routines into this folder—just instantiate and call them from your server routing logic.

---

## 🏃 Running It

1. **Compile everything**

   ```bash
   javac *.java routines/*.java
   ```
2. **Start the server**

   ```bash
   java Main
   ```
3. **(Optional)** Launch `ProtoClient` in another shell to chat with your server.


Or you could legit just
```bash
java Main.java
```
---



## 🤔 Next Steps

- Swap out the `Dummy` + `DataBindings` combo for a real database driver.
- Define new routines in `routines/` (e.g. `Calculator`, `WeatherFetcher`).
- Enhance `Protocol.java` to support richer formats (JSON, Protobuf, etc.).
- Add authentication, robust error handling, and graceful shutdown hooks.

Enjoy hacking on this rag‑tag server–client model! 🚀
