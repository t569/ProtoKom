import server.*;

public class Main {
    
    public static void main(String[]args)
    {
        final int port = 9001;
        ProtoServer haze = new ProtoServer(port); 
        try 
        {
            haze.start(); 
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }

        ProtoClient client1 = new ProtoClient("localhost", port, "kome");

    }
}
