import java.io.*;
import java.net.*;
import routines.Echo;

public class ProtoClient {
    private Socket socket;
    private ObjectInputStream in;
    private ObjectOutputStream out;
    private String clientId;
    private String hostName;
    private Echo client_parrot;
    public ProtoClient(String host, int port, String clientId)
    {
        // init the parrot
        client_parrot = new Echo(clientId);
        try 
        {
            this.socket = new Socket(host, port);
            this.out = new ObjectOutputStream(socket.getOutputStream());
            this.in = new ObjectInputStream(socket.getInputStream());
            this.clientId = clientId;
            this.hostName = host;


            // then initialise the handshake
            handshake(socket, in, out);

            // all the extra request sending methods are coded independently
        }
        catch(IOException e)
        {
            client_parrot.log_err_with_ret(e);
        }
    }

    public String getClientId()
    {
        return this.clientId;
    }

    // just send a regular request
    public void sendRequest(Protocol req, ObjectOutputStream out) throws IOException, ClassNotFoundException
    {
        out.writeObject(req);
        out.flush();

        if(req.getStatus() == Status.CONN_DISCONNECT)
        {
            client_parrot.log("Closing connection with " + hostName);
            // close the socket and end connection; this is done after sending the request
            this.socket.close();
        }
    }


    // get a response handle it and return a protocol to send
    public Protocol getResponse(ObjectInputStream in, Socket socket) throws IOException, ClassNotFoundException
    {
        client_parrot.log("Response from server: " + hostName);
        return (Protocol) in.readObject();

    }

    // TODO: now we parse the response by getting the metadata 
    // we can also do ther stuff with the protocol fields


    public void handshake(Socket socket, ObjectInputStream in, ObjectOutputStream out)
    {
        // handle the client server handshake on the client side
        
        try
        {
            // 1. send the name of the client first
            out.writeObject(getClientId());
            out.flush();


            // 2. get the intended CONN_INIT_HANDSHAKE response
            Protocol init_handshake_response = (Protocol) in.readObject();

            if(init_handshake_response.getStatus() == Status.CONN_INIT_HANDSHAKE)
            {
                // 3. send a connection request CONN_REQ
                out.writeObject(
                    new Protocol(
                        Status.CONN_REQ,
                        new Protocol.Packet(
                            clientId,
                            hostName,
                            "CONNECTION REQUEST",
                            new Protocol.Packet.MetaData()
                        )
                    )
                );
                client_parrot.log("Connection request sent to: " + hostName);
                out.flush();
            }

            else
            {
                // error : abort connection
            }


            // 5. expect a CONN_ACK response
            Protocol conn_ack_response = (Protocol) in.readObject();

            if(conn_ack_response.getStatus() == Status.CONN_ACK)
            {
                // the connection is okay
                client_parrot.log("Connection Acknowledge recieved");
            }

            else
            {
                // error : abort connection and close; you were kicked
                if(conn_ack_response.getStatus() == Status.CONN_BOOT)
                {
                    client_parrot.log("Booted by server: " + hostName +
                                     "\n" + "Reason: "+
                                      conn_ack_response.getPacket().getText().substring(5));

                    socket.close();     
                }

                else
                {
                    // just abort for any other response type
                    // i believe the socket automatically closes here?


                    // TODO: add logging to specify the type of connection
                    throw new Exception("Connection type other than CONN_ACK ");
                }
            }

        }
        catch(Exception e)
        {
            // catch the error
            client_parrot.log_err_with_ret(e);
        }

        
    }
}
