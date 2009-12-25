/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package games.stendhal.client.sound.system.Processors;

import games.stendhal.client.sound.system.SignalProcessor;
import java.io.InputStream;
import java.io.IOException;
import com.jcraft.jogg.*;
import com.jcraft.jorbis.*;
/**
 *
 * @author silvio
 */
public class OggVorbisDecoder extends SignalProcessor
{
    private StreamState mOggStreamState = null;
    private SyncState   mOggSyncState   = null;
    private DspState    mVorbisDspState = null;
    private Block       mVorbisBlock    = null;
    private Comment     mVorbisComment  = null;
    private Info        mVorbisInfo     = null;

    private InputStream mIStream         = null;
    private float[]     mOutputBuffer    = null;
    private byte[]      mInputBuffer     = null;
    private int         mInputBufferSize = 0;
    private int         mReadPos         = 0;
    private boolean     mEndOfStream     = true;
    private boolean     mDecoderIsOpened = false;
    
    protected void init(InputStream stream, int inputBufferSize, int outputNumSamplesPerChannel) throws IOException
    {
        mOggStreamState = new StreamState();
        mOggSyncState   = new SyncState();
        mVorbisDspState = new DspState();
        mVorbisBlock    = new Block(mVorbisDspState);
        mVorbisComment  = new Comment();
        mVorbisInfo     = new Info();
        mReadPos        = 0;
        mEndOfStream    = false;
        
        mOggSyncState.init();
        mOggSyncState.buffer(inputBufferSize);  // add "inputBufferSize" bytes to the buffer
        mInputBuffer      = mOggSyncState.data; // get a reference to the buffer
        mInputBufferSize  = inputBufferSize;
        mIStream          = stream;

        if(!readHeader())
            throw new IOException("could not read ogg headers");

        // IMPORTANT: read the header first before setting up the output buffer
        mOutputBuffer    = new float[outputNumSamplesPerChannel * getNumChannels()];
        mDecoderIsOpened = true;
    }

    protected Page readPage(boolean ignoreHoles, boolean updateStreamState) throws IOException
    {
        Page oggPage = new Page();

        while(!mEndOfStream)
        {
            // try to get a page from the buffer
            switch(mOggSyncState.pageout(oggPage))
            {
            case -1: // the page couldn't be read, because the data has a hole
                if(!ignoreHoles)
                    return null;
                // if we ignore holes we continue to "case 0:"

            case 0: // we read to few to get a page
                {
                    int numBytesRead = mIStream.read(mInputBuffer, mReadPos, mInputBufferSize); // read one data block into the buffer

                    if(numBytesRead <= 0) // it's the quit of the stream
                    {
                        mEndOfStream = true;
                        return null;
                    }
                    
                    mOggSyncState.wrote(numBytesRead); // say syncState how many bytes we have read
                }
                break;

            case 1: // we've got a page
                {
                    if(updateStreamState)
                        if(mOggStreamState.pagein(oggPage) == -1)
                            return null;

                    if(oggPage.eos() != 0)
                        mEndOfStream = true;
                    
                    return oggPage;
                }
            }

            mReadPos     = mOggSyncState.buffer(mInputBufferSize); // add "mInputBufferSize" bytes to the buffer
            mInputBuffer = mOggSyncState.data;                     // get a reference to the buffer
        }

        return null;
    }
    
    protected Packet readPacket(boolean ignoreHoles) throws IOException
    {
        Packet oggPacket = new Packet();

        while(true)
        {
            switch(mOggStreamState.packetout(oggPacket))
            {
            case -1: // the packet couldn't be read, because the data has a hole
                if(!ignoreHoles)
                    return null;
                // if we ignore holes we continue to "case 0:"

            case 0: // we read to few to get a packet
                if(readPage(ignoreHoles, true) == null)
                    return null;
                break;

            case 1:
                return oggPacket;
            }
        }
    }

    protected boolean readHeader() throws IOException
    {
        Page firstOggPage = readPage(false, false);

        if(firstOggPage == null)
            return false;

        mOggStreamState.init(firstOggPage.serialno());
        mOggStreamState.reset();
        mVorbisInfo.init();
        mVorbisComment.init();

        if(mOggStreamState.pagein(firstOggPage) == -1)
            return false;

        // read three packets to get the header
        for(int i=0; i<3; ++i)
        {
            Packet oggPacket = readPacket(false);

            if(oggPacket == null)
                return false;

            if(mVorbisInfo.synthesis_headerin(mVorbisComment, oggPacket) < 0)
                return false; // error while interpreting packet data
        }

        mVorbisDspState.synthesis_init(mVorbisInfo);
        mVorbisBlock.init(mVorbisDspState);
        return true;
    }

    protected int read() throws IOException
    {
        int         outputBufferSize           = mOutputBuffer.length;
        int         outputNumSamplesPerChannel = outputBufferSize / mVorbisInfo.channels;
        float[][][] uniformPCMData             = new float[1][][];
        int[]       PCMIndex                   = new int[mVorbisInfo.channels];

        int receivedNumSamples       = 0;
        int numSamplesReadPerChannel = 0;
        int sampleIdx                = 0;

        while(numSamplesReadPerChannel < outputNumSamplesPerChannel)
        {
            // if we read all pcm data from "uniformPCMData"
            if(sampleIdx >= receivedNumSamples)
            {
                //System.out.println("range: " + receivedNumSamples);
                mVorbisDspState.synthesis_read(receivedNumSamples); // tell dsp-state that we read all samples

                if(reachedEndOfStream()) // no more pcm data to get
                    break;

                // receive the next chunk of uniform pcm data
                receivedNumSamples = mVorbisDspState.synthesis_pcmout(uniformPCMData, PCMIndex);
                sampleIdx          = 0; // reset sample index

                // if there are no more samples in the packet, we read in a new packet from the stream
                if(receivedNumSamples == 0)
                {
                    Packet oggPacket = readPacket(true);

                    if(oggPacket == null || mVorbisBlock.synthesis(oggPacket) != 0)
                        continue; // if the packet we read from the stream is corrupted or has no audio data, we ignore it

                    mVorbisDspState.synthesis_blockin(mVorbisBlock);
                }
            }
            else
            {
                int outputIndex = numSamplesReadPerChannel * mVorbisInfo.channels;

                for(int c=0; c<mVorbisInfo.channels; ++c)
                {
                    float outputValue = uniformPCMData[0][c][PCMIndex[c] + sampleIdx];                    
                    mOutputBuffer[outputIndex + c] = outputValue;
                }

                ++numSamplesReadPerChannel;
                ++sampleIdx;
            }
        }

        mVorbisDspState.synthesis_read(sampleIdx);
        return numSamplesReadPerChannel;
    }

    protected float[] getOutputBuffer()
    {
        return mOutputBuffer;
    }

    @Override
    protected boolean generate()
    {
        if(reachedEndOfStream())
        {
            super.quit();
            return false;
        }
        
        try
        {
            int numSamples = read();
            super.propagate(mOutputBuffer, numSamples, getNumChannels(), getSampleRate());
        }
        catch(IOException e)
        {
            close();
        }

        return true;
    }

    // --------------------- public interface ---------------------- //

    public OggVorbisDecoder(InputStream stream, int inputBufferSize, int outputNumSamplesPerChannel) throws IOException
    {
        init(stream, inputBufferSize, outputNumSamplesPerChannel);
    }

    public synchronized int     getNumChannels    () { return mVorbisInfo.channels; }
    public synchronized int     getSampleRate     () { return mVorbisInfo.rate;     }
    public synchronized boolean reachedEndOfStream() { return mEndOfStream;         }

    public synchronized void open(InputStream stream, int inputBufferSize, int outputNumSamplesPerChannel) throws IOException
    {
        if(!mDecoderIsOpened)
            init(stream, inputBufferSize, outputNumSamplesPerChannel);
    }

    public synchronized void close()
    {
        mOggStreamState.clear();
        mOggSyncState.clear();
        mVorbisBlock.clear();
        mVorbisDspState.clear();
        mVorbisInfo.clear();

        try
        {
            mIStream.close();
        }
        catch(IOException exception)
        {
            assert false: exception.toString();
        }
        
        mOutputBuffer    = null;
        mIStream         = null;
        mDecoderIsOpened = false;
        mEndOfStream     = true;
    }
}
