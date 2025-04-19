import java.io.*;
import java.net.*;


public class ProtoClient {
    private Socket socket;
    private ObjectInputStream in;
    private ObjectOutputStream out;
    private String clientId;
    private String hostName;

    public ProtoClient(String host, int port, String clientId)
    {
        try 
        {
            this.socket = new Socket(host, port);
            this.out = new ObjectOutputStream(socket.getOutputStream());
            this.in = new ObjectInputStream(socket.getInputStream());
            this.clientId = clientId;

            // then initialise the handshake
            handleServer();
        }
        catch(IOException | ClassNotFoundException e)
        {
            e.printStackTrace();
        }
    }

    // yes this is terrible naming im sorry, i was listening music and was high off some stuff lmao
    public void handleServer() throws IOException, ClassNotFoundException
    {
        // 1. Expect the CONN_INIT_HANDSHAKE signal
        Protocol trig = (Protocol) in.readObject();
        hostName = trig.getPacket().getSender();

        if(trig.getStatus() != Status.CONN_INIT_HANDSHAKE)
        {
            if(trig.getStatus() == Status.CONN_OK)
            {
                // we are already connected
                return;
            }
            else 
            {
                throw new IOException("Handshake failed: no CONN_INIT_HANDSHAKE");
            }
        }
 
        // 2. Send CONN_REQ
        else
        {
            out.writeObject(new Protocol(
                Status.CONN_REQ,
                new Protocol.Packet(
                    clientId,                       /* sender */
                    hostName,                       /* reciever */
                    "CONNECTION REQUEST",
                    new Protocol.Packet.MetaData()
                    )
            ));
            out.flush();
        }

        // 3. Expect CONN_ACK
        Protocol ack = (Protocol) in.readObject();
        if(ack.getStatus() != Status.CONN_ACK)
        {
            throw new IOException("Handshake failed: no CONN_ACK");
        }


        // Has been acknowledged
        
        // 4. Send CONN_CONF confirmation test
        out.writeObject(new Protocol(
            Status.CONN_CONF,
            new Protocol.Packet(
                clientId,
                hostName,
                "CONFIRM CONNECT",
                new Protocol.Packet.MetaData()
            )
        ));
        out.flush();

        // 5. Proceed or get CONN_BOOT
        Protocol result = (Protocol) in.readObject();
        String txt = result.getPacket().getText();

        if(txt.startsWith("BOOT"))
        {
            throw new IOException("Booted by server: "+hostName+ " Reason: "+txt.substring(5));
        }
        else
        {
            // we were successful: we got a CONN_ACK or CONN_OK
            System.out.println("Successful proxy");
        }   
    }

    // just send a regular request
    public void sendRequest(Protocol req, ObjectOutputStream out) throws IOException, ClassNotFoundException
    {
        out.writeObject(req);
        out.flush();
    }


    // get a response handle it and return a protocol to send
    public Protocol getResponse(ObjectInputStream in) throws IOException, ClassNotFoundException
    {
        return (Protocol) in.readObject();

    }

    // TODO: now we parse the response by getting the metadata 
    // we can also do ther stuff with the protocol fields

}
