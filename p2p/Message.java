package p2p;

import java.util.*;

import java.io.*;
import java.nio.*;

public class Message {
    private int messageLength;
    private char messageType;
    private byte[] messagePayload;

    public Message() {

    }

    public Message(char messageType) {
        this.messageType = messageType;
        this.messageLength = 1;
        this.messagePayload = new byte[0];
    }

    public Message(char messageType, byte[] messagePayload) {
        this.messageType = messageType;
        this.messagePayload = messagePayload;
        this.messageLength = this.messagePayload.length + 1;
    }

    public int getMessageLength() {
        return messageLength;
    }

    public void setMessageLength(int messageLength) {
        this.messageLength = messageLength;
    }

    public char getMessageType() {
        return this.messageType;
    }

    public void setMessageType(char messageType) {
        this.messageType = messageType;
    }

    public byte[] getMessagePayload() {
        return messagePayload;
    }

    public void setMessagePayload(byte[] messagePayload) {
        this.messagePayload = messagePayload;
    }

    public byte[] constructMessage() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            byte[] messageBytes = ByteBuffer.allocate(4).putInt(this.messageLength).array();
            baos.write(messageBytes);
            baos.write((byte) this.messageType);
            baos.write(this.messagePayload);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return baos.toByteArray();
    }

    public void setMessage(int len, byte[] message) {
        this.messageLength = len;
        this.messageType = getMessageTypeAsChar(message, 0);
        this.messagePayload = getPayload(message, 1);
    }

    public char getMessageTypeAsChar(byte[] message, int index) {
        return (char) message[index];
    }

    public byte[] getPayload(byte[] message, int index) {
        byte[] payload = new byte[this.messageLength - 1];
        System.arraycopy(message, index, payload, 0, this.messageLength - 1);
        return payload;
    }

    public int getPieceIndexFromPayload() {
        return convertByteArrayToInteger(this.messagePayload, 0);
    }

    public int convertByteArrayToInteger(byte[] message, int start) {
        byte[] len = new byte[4];
        for (int i = 0; i < 4; i++) {
            len[i] = message[i + start];
        }
        ByteBuffer buffer = ByteBuffer.wrap(len);
        return buffer.getInt();
    }

    public BitSet getBitFieldMessage() {
        return BitSet.valueOf(this.messagePayload);
    }

    public byte[] getPieceFromPayload() {
        int size = this.messageLength - 5;
        byte[] piece = new byte[size];
        for (int i = 0; i < size; i++) {
            piece[i] = this.messagePayload[i + 4];
        }
        return piece;
    }
}
