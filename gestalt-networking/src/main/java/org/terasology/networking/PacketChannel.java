package org.terasology.networking;

import io.netty.buffer.ByteBuf;
import io.netty.channel.socket.DuplexChannel;

public class PacketChannel {
    private final DuplexChannel channel;

    double time;
    float packetLost;
    float sendBandwidthKbps;
    float receivedBandwidthKbps;
    float ackedBandwidthKbps;

    int sequence;
    int numberAcks;
    byte[] acks;

    SequenceBuffer<SentPacketData> SentPacketBuffer;
    SequenceBuffer<ReceivedPacketData> receivedPacketBuffer;
    SequenceBuffer<FragmentReassemblyData> fragmentReassemblyBuffer;

    public static class SequenceBuffer<T> {
        int sequence;
        T[] entry;
        int[] entry_sequence;

        public T getEntry(int seq) {
            return entry[seq % entry.length];
        }

        public int getSequence(int seq) {
            return this.entry_sequence[seq % entry_sequence.length];
        }

        public int getAcks() {
            return sequence - 1;
        }

        public int ackBits() {
            int bits = 0;
            for(int i = 0; i < 32; ++i) {
                int sequence = getAcks() - i;
                if(getSequence(sequence) == sequence) {
                    bits |= 1;
                }
                bits <<= 1;
            }
            return bits;
        }
    }

    public static class SentPacketData {
        public double time;
        public boolean acked;
        public int packetBytes;
    }

    public static class ReceivedPacketData {
        public double time;
        public int packetBytes;
    }

    public static class FragmentReassemblyData {
        public short sequence;
        public short ack;
        public int acks;
        public int numberFragmentsReceived;
        public int numberFragmentsTotal;
        public ByteBuf packedData;
        public int packedBytes;
        public int packedHeaderBytes;
        public ByteBuf fragmentReceived;
    }
    public int generateAckBits(int ackBits) {
        return 0;
    }



    public PacketChannel(DuplexChannel channel) {
        this.channel = channel;
    }
}
