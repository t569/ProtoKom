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
            // handleServer();

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
            // close the socket and end connection; this is done after sending the request
            this.socket.close();
        }
    }


    // get a response handle it and return a protocol to send
    public Protocol getResponse(ObjectInputStream in) throws IOException, ClassNotFoundException
    {
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

     // yes this is terrible naming im sorry, i was listening to music and was high off some stuff lmao
    //  public void handleServer() throws IOException, ClassNotFoundException
    //  {
    //      // 1. Expect the CONN_INIT_HANDSHAKE signal
 
    //      // ERROR: lets deal with this
    //      Protocol trig = (Protocol) in.readObject();
    //      client_parrot.log(trig.packet.getSender());
    //      hostName = trig.getPacket().getSender();
 
    //      if(trig.getStatus() != Status.CONN_INIT_HANDSHAKE)
    //      {
    //          if(trig.getStatus() == Status.CONN_OK)
    //          {
    //              client_parrot.log(clientId + "message session fine. From: " + hostName);
    //              // we are already connected
    //              return;
    //          }
    //          else 
    //          {
    //              throw new IOException("Handshake failed: no CONN_INIT_HANDSHAKE");
    //          }
    //      }
  
    //      // 2. Send CONN_REQ
    //      else
    //      {
    //          out.writeObject(new Protocol(
    //              Status.CONN_REQ,
    //              new Protocol.Packet(
    //                  clientId,                       /* sender */
    //                  hostName,                       /* reciever */
    //                  "CONNECTION REQUEST",
    //                  new Protocol.Packet.MetaData()
    //                  )
    //          ));
    //          out.flush();
    //          client_parrot.log("Connection Request Sent");
    //      }
 
    //      // 3. Expect CONN_ACK
    //      Protocol ack = (Protocol) in.readObject();
    //      if(ack.getStatus() != Status.CONN_ACK)
    //      {
    //          throw new IOException("Handshake failed: no CONN_ACK");
    //      }
 
 
    //      // Has been acknowledged
         
    //      // 4. Send CONN_CONF confirmation test
    //      out.writeObject(new Protocol(
    //          Status.CONN_CONF,
    //          new Protocol.Packet(
    //              clientId,
    //              hostName,
    //              "CONFIRM CONNECT",
    //              new Protocol.Packet.MetaData()
    //          )
    //      ));
    //      out.flush();
 
    //      // 5. Proceed or get CONN_BOOT
    //      Protocol result = (Protocol) in.readObject();
 
    //      // this is bad code im sorry
    //      String message = result.getPacket().getText().substring(5);
    //      if(result.getStatus() == Status.CONN_BOOT)
    //      {
    //          throw new IOException("Booted by server: "+hostName+ " Reason: "+message);
    //      }
    //      else
    //      {
    //          // we were successful: we got a CONN_ACK or CONN_OK
    //          // System.out.println("Successful proxy");
 
    //          client_parrot.log("Successful proxy");
    //      }   
    //  }
}
