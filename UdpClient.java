/*****************************************
*   Alfredo Ceballos
*   CS 380 - Computer Networks
*   Project 3
*   Professor Nima Davarpanah
*****************************************/

import java.io.*;
import java.net.*;

public class UdpClient {
    
    public static short checksum(byte[] bytes) {
        
        int b = (bytes.length + (bytes.length % 2)) / 2;    // pairs of bytes
        short s;    // temp short
        int sum = 0;    // 32 bit storage space
        
        for (short i = 0; i < b; i++) { 
            // sum pairs of bytes
            s = (short) ((bytes[i * 2] & 0xFF) << 8);   // align bytes in short
            
            if ((i * 2) + 1 < bytes.length) {
                s += (bytes[(i * 2) + 1] & 0xFF);
            }
            
            sum += (s & 0xFFFF);    // add new short to sum
            
            // If overflow occurs...
            if ((sum & 0xFFFF0000) > 0) {
                sum &= 0x0000FFFF;  // drop first 16 bits
                sum++;              // wrap around overflow
            }
        }
        
        s = (short) ~(sum & 0x0000FFFF);    // one's complement
        return s;   // return checksum
    }// End of checksum
    
    
    public static byte[] IPv4Packet(byte[] data) throws Exception {
        
        byte[] header = new byte[20];
            
        header[0] = 69; // 01000101 : Version 0100, HLen 0101
        header[1] = 0;  // TOS
        
        short length;
        length = (short) (data.length + header.length);
        header[2] = (byte) ((length & 0xFF00) >> 8); // Length
        header[3] = (byte) (length & 0x00FF);
        
        header[4] = 0;  // Identification 
        header[5] = 0;
        header[6] = 64; // 01000000 : Flags 010, Offset 00000
        header[7] = 0;  // Offset
        header[8] = 50; // TTL : 50 seconds
        header[9] = 17; // Protocol : 17 for UDP
        
        // skip checksum until after header is filled
        
        byte[] temp = getActualIP();
        for(int i = 0; i < temp.length; i++) {
            header[i + 12] = temp[i];
        }
        
        header[16] = (byte) 18;
        header[17] = (byte) 221;
        header[18] = (byte) 102;
        header[19] = (byte) 182;    // destination address bytes
        
        short cks = checksum(header);   // checksum of header
        header[10] = (byte) ((cks & 0xFF00) >> 8);
        header[11] = (byte) (cks & 0x00FF);
        
        
        // The packet's header data is in the header byte array, here
        // it is copied into the packet byte array, which is the size of 
        // of the header plus the size of the data generated, that data is then
        // also copied in at the end
        byte[] packet = new byte[header.length + data.length];
        for (int j = 0; j < header.length; j++) {packet[j] = header[j];}
        for (int k = 0; k < data.length; k++) {packet[header.length + k] = data[k];}
        return packet;
        
    } // End of IPv4Packet
    
    
    // Fetching client's actual IP address and converting it into 
    // byte values, then inserting them into the packet
    public static byte[] getActualIP() {
        byte[] ipAddress = new byte[4];
        try {
            URL getIP = new URL("http://checkip.amazonaws.com");
            BufferedReader in = new BufferedReader(new InputStreamReader(getIP.openStream(), "UTF-8"));
            String[] src = in.readLine().split("\\.");
            for (int i = 0; i < src.length; i++) {
                int j = Integer.parseInt(src[i]);
                ipAddress[i] = (byte) j;  // source address bytes
            }
        }catch(Exception e) {
            System.out.println("It broke...");
        }
        return ipAddress;
    }
    
    
    // Since the data to be added at the end of the IPv4 packet is in powers
    // of 2, this method generates a byte array who's size if a power of 2, 
    // filled with random data
    public static byte[] genByteArray(int n) {
        int numBytes = (int)Math.pow(2, n);
        byte[] arr = new byte[numBytes];
        
        for(int i = 0; i  < numBytes; i++){
            arr[i] = (byte)(Math.random() * 255);
        }
        
        return arr;
    }
    
    
    // Generates UDP packet
    public static byte[] genUDP(int destPort, int dataSize) {
        byte[] randData = genByteArray(dataSize);
        int dataLength = randData.length;
        byte[] udpPacket = new byte[8 + dataLength];
        
        udpPacket[0] = 0; // Source port
        udpPacket[1] = 0;
        
        udpPacket[2] = (byte)(destPort >> 8); // Destination port
        udpPacket[3] = (byte)(destPort);
        
        udpPacket[4] = (byte)(dataLength >> 8); // Length of data
        udpPacket[5] = (byte) dataLength;
        // elements 6 and 7 hold the checksum, which needs a psuedo header
        // to be calculated, elements 8 and on contain the data
        for(int i = 0; i < dataLength; i++) {
            udpPacket[i + 8] = randData[i];
        }
        
        
        byte[] pHeader = new byte[12]; // Psuedo-header
        byte[] temp = getActualIP(); // Getting source address
        for(int i = 0; i < temp.length; i++) {
            pHeader[i] = temp[i]; // Sticking source address to pseudo header
        }
        
        pHeader[4] = (byte)18; //Destination address
        pHeader[5] = (byte)221;
        pHeader[6] = (byte)102;
        pHeader[7] = (byte)182;
        
        pHeader[8] = (byte)0; // Reserved
        pHeader[9] = 17; // Protocol

        pHeader[10] = (byte)(dataLength >> 8); // Data length
        pHeader[11] = (byte)dataLength;
        
        // The header that gets sent to the checksum is made up of the
        // udp packet and the pseudo header
        int tempSize = 8 + 12 + dataLength;
        byte[] checksumPacket = new byte[tempSize];
        int j = 0;
        for (int i = 0; i < tempSize; i++) {
            if (i < (8 + dataLength)) {
                checksumPacket[i] = udpPacket[i];
            } else {
                checksumPacket[i] = pHeader[j];
                j++;
            }
        }
        short checksum = checksum(checksumPacket);
        udpPacket[6] = (byte) ((checksum >> 8) & 0xFF);
        udpPacket[7] = (byte) (checksum & 0xFF);
        return udpPacket;
        } // End of genUDP
    
    
    public static void main(String[] args) throws Exception {
        try (Socket socket = new Socket("18.221.102.182", 38005)) {
            // Creating client input/output streams to receive 
            // and send messages from and to server 
            InputStream is = socket.getInputStream();
            InputStreamReader isr = new InputStreamReader(is);
            OutputStream os = socket.getOutputStream();
            
            // "Handshaking" process, hard-coding 0xDEADBEEF
            byte[] b  = {(byte)0xDE, (byte)0xAD, (byte)0xBE,(byte)0xEF};
            byte[] packet = IPv4Packet(b);
            os.write(packet);
            System.out.print("Handshake response: 0x");
            for(int i = 0; i < 4; i++){
                System.out.printf("%02X", is.read());
            }
            int portNum = (is.read() << 8);
            portNum += is.read();
            System.out.printf("%nPort number received: %d%n", portNum);
            
            float totalRTT = 0;
            for(int i = 1; i <= 12; i++) {
                long sent, received, rtt;
                String response = "";
                byte[] udpPacket = genUDP(portNum, i);
                byte[] iPV4Packet = IPv4Packet(udpPacket);
                
                System.out.printf("Sending packet with %d byte of data%n", (int)Math.pow(2, i));
                sent = System.currentTimeMillis(); // Data about to be sent                
                os.write(iPV4Packet);
                for(int j =  0; j < 4; j++){
                    response += Integer.toHexString(is.read()).toUpperCase();
                }
                // Data received and processed
                received = System.currentTimeMillis();
                System.out.println("0x" + response);
                rtt = received - sent;
                System.out.printf("RTT: %dms%n%n", rtt);
                totalRTT += rtt;
            }
            System.out.printf("Average RTT: %.2fms", (totalRTT / 12));
            
        } // End of try
    }// End of main
    
}// End of file