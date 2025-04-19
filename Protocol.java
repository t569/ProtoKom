/* 
 * Hello Guys, Welcome to my Shitty Network Protocol
 * I made this to make my Sockets Smarter Lmao
 */

import java.io.Serializable;
import java.util.Optional;

enum Status
{                           // Sent by: 
    CONN_INIT_HANDSHAKE,    // server
    CONN_BOOT,  // server
    CONN_REQ,   // client
    CONN_ACK,   // server
    CONN_CONF,  // client
    CONN_OK,    // server
    CONN_DISCONNECT // client
}

public class Protocol implements Serializable{
    
    // in general every protocol is made up of the following

    // A Status and a message
    private Status status;
    public Packet packet;
    public Protocol(Status status, Packet packet)
    {
        this.status = status;
        this.packet = packet;
    }

    public Packet getPacket()
    {
        return this.packet;
    }

    public Status getStatus()
    {
        return this.status;
    }

    public static class Packet implements Serializable
    {
        // Now this class has some extra stuff

        // The sender, reciever, text amd metadata
        private String sender;
        private String reciever;
        private String text;
        private MetaData metadata;

        public Packet(String sender, String reciever, String text, MetaData metadata)
        {
            this.sender = sender;
            this.reciever = reciever;
            this.text = text;
            this.metadata = metadata;
        }

        public String getSender()
        {
            return this.sender;
        }

        public String getReciever()
        {
            return this.reciever;
        }

        public String getText()
        {
            return this.text;
        }

        public MetaData getMetaData()
        {
            return this.metadata;
        }


        // This has the commands, still thinking about it
        // Basically tells the reciever what to do with the message
        public static class MetaData implements Serializable
        {
            enum CommProtocol
            {
                GET,
                POST,
                UPDATE,
                DELETE,
            }
            private CommProtocol comm_protocol;
            private Object payload;
            public MetaData()
            {
                // TODO: what should be here?
                this.comm_protocol = null;  // useful during the handshake phase
                this.payload = null;
            }

            public MetaData(CommProtocol comm_protocol, Object payload)
            {
                this.comm_protocol = comm_protocol;
                this.payload = payload;
            }


            // Please note that this can return null so we use optional
            // we have to known the type of the payload we are fetching
            public <T> Optional<T> getPayload(Class <T> type)
            {   
                return Optional.ofNullable(type.cast(payload));
            }

            public Optional<CommProtocol> geCommProtocol()
            {
                return Optional.ofNullable(comm_protocol);
            }

        }
    }

    /* Soooooooo
     * This is the protocol in a nutshell
     * 
     */

    
}
