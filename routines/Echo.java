package routines;

public class Echo {
    private static final String implicit = "Echo";
    private String name;
    public Echo(String name)
    {
        this.name = name;
    }

    public Echo()
    {
        this.name = implicit;
    }
    public String echo(String message)
    {
        // log to the console and output to a string
        String formatting = "[ " + name + " ]" + ": " + message;
        log(message);
        return formatting;
    }

    public void log(String message)
    {
        String formatting = "[ " + name + " ]" + ": " + message;
        System.out.println(formatting);
    }

    public String log_err_with_ret(Exception e)
    {
        String formatting = "[ " + name + " ]" + ": " + e.getMessage();
        System.out.println(formatting);
        return formatting;
    }
}   

