package com.nesc.NettyServer;

import io.netty.buffer.ByteBuf;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Unit test for simple App.
 */
public class AppTest 
    extends TestCase
{
//	public ByteBuf[] b= {120,-124};
//	public long l;
    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public AppTest( String testName )
    {
        super( testName );
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite()
    {
        return new TestSuite( AppTest.class );
    }

    /**
     * Rigourous Test :-)
     */
    public void testApp()
    {
        assertTrue( true );
    }

//	public static void main(String[] args){
//		ByteBufferTest byteb = new ByteBufferTest();
//		byteb.l = (long)(byteb.b[0]<<8)|(long)byteb.b[1];
//		System.out.println(byteb.b[0]);
//		System.out.println(byteb.b[1]);
//		System.out.println(byteb.l);
//	}
}
