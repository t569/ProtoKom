package tests;
import server.ProtoClient;
import server.ProtoServer;
import server.Protocol;


// test passed
public class DisconnectServer
{
    public static void main(String[]args)
    {
        final int port = 9001;
        ProtoServer server = new ProtoServer(port);

        try 
        {
            server.start();
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }

        ProtoClient client1 = new ProtoClient("localhost", port, "client1");


        // send a disconnect request

        // request to be sent
        Protocol req = new Protocol(
                        Protocol.Status.CONN_DISCONNECT,
                        new Protocol.Packet(
                            client1.getClientId(),
                            client1.getHostName(),
                            "DISCONNECT",
                            new Protocol.Packet.MetaData()
                        )
                        );

        try 
        {
            client1.sendRequest(req);
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
        finally
        {
            server.stop();
        }
        
    }
}