/**

Copyright (C) SYSTAP, LLC 2006-2010.  All rights reserved.

Contact:
     SYSTAP, LLC
     4501 Tower Road
     Greensboro, NC 27410
     licenses@bigdata.com

This program is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; version 2 of the License.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package com.bigdata.journal.ha;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.bigdata.io.DirectBufferPool;
import com.bigdata.io.TestCase3;

/**
 * Test the raw socket protocol implemented by HASendService and
 * HAReceiveService.
 * 
 * @author martyn Cutcher
 * 
 * @todo Add random interrupts of the threads and shutdown of the services to
 *       look for deadlock conditions.
 */
public class TestHASendAndReceive extends TestCase3 {

	/**
	 * A random number generated - the seed is NOT fixed.
	 */
	protected final Random r = new Random();

	/**
	 * Returns random data that will fit in N bytes. N is chosen randomly in
	 * 1:256.
	 * 
	 * @return A new {@link ByteBuffer} wrapping a new <code>byte[]</code> of
	 *         random length and having random contents.
	 */
	public ByteBuffer getRandomData() {

		final int nbytes = r.nextInt(256) + 1;

		return getRandomData(nbytes);

	}

    /**
     * Returns random data that will fit in <i>nbytes</i>.
     * 
     * @return A new {@link ByteBuffer} wrapping a new <code>byte[]</code>
     *         having random contents.
     */
    public ByteBuffer getRandomData(final int nbytes) {

        final byte[] bytes = new byte[nbytes];

        r.nextBytes(bytes);

        return ByteBuffer.wrap(bytes);

    }

    /**
     * Returns random data that will fit in <i>nbytes</i>.
     * 
     * @return A new {@link ByteBuffer} wrapping a new <code>byte[]</code>
     *         having random contents.
     */
    public ByteBuffer getRandomData(final ByteBuffer b, final int nbytes) {

        final byte[] a = new byte[nbytes];

        r.nextBytes(a);
        
        b.limit(nbytes);
        b.position(0);
        b.put(a);
        
        b.flip();
        
        return b;

    }
    
	public TestHASendAndReceive() {

	}
	
	private HASendService sendService;
	private HAReceiveService<HAWriteMessage> receiveService;
	
	protected void setUp() throws Exception {

	    final InetSocketAddress addr = new InetSocketAddress(3000);
		
		receiveService = new HAReceiveService<HAWriteMessage>(addr, null);
		receiveService.start();

        sendService = new HASendService(addr);

    }

    protected void tearDown() throws Exception {

        if (receiveService != null)
            receiveService.terminate();

        if (sendService != null)
            sendService.terminate();
	    
	}

    /**
     * Should we expect concurrency of the Socket send and RMI? It seems that we
     * should be able to handle it whatever the logical argument. The only
     * constraint should be on the processing of each pair of socket/RMI
     * interactions. OTOH, if we are intending to process the OP_ACCEPT and
     * OP_READ within the ReadTask that can only be processed AFTER the RMI is
     * received, then we should not sen the socket until we have a returned
     * FutureTask.
     * 
     * @throws ExecutionException
     * @throws InterruptedException
     */
    public void testSimpleExchange() throws InterruptedException, ExecutionException {
       
        final ByteBuffer tst1 = getRandomData(50);
        // sendService.send(tst1);

        final HAWriteMessage msg1 = new HAWriteMessage(50, 0);
        final HAWriteMessage msg2 = new HAWriteMessage(100, 0);
        final ByteBuffer rcv = ByteBuffer.allocate(2000);
        final ByteBuffer rcv2 = ByteBuffer.allocate(2000);

        {
            rcv.limit(50);
            final Future<Void> futRec = receiveService.receiveData(msg1, rcv);
            final Future<Void> futSnd = sendService.send(tst1);
            while (!futSnd.isDone() && !futRec.isDone()) {
                try {
                    futSnd.get(10L, TimeUnit.MILLISECONDS);
                } catch (TimeoutException ignore) {
                }
                try {
                    futRec.get(10L, TimeUnit.MILLISECONDS);
                } catch (TimeoutException ignore) {
                }
            }
            futSnd.get();
            futRec.get();
            assertEquals(tst1, rcv);
        }

        {
            final ByteBuffer tst2 = getRandomData(100);
            rcv2.limit(100);
            final Future<Void> futRec = receiveService.receiveData(msg2, rcv2);
            final Future<Void> futSnd = sendService.send(tst2);
            while (!futSnd.isDone() && !futRec.isDone()) {
                try {
                    futSnd.get(10L, TimeUnit.MILLISECONDS);
                } catch (TimeoutException ignore) {
                }
                try {
                    futRec.get(10L, TimeUnit.MILLISECONDS);
                } catch (TimeoutException ignore) {
                }
            }
            futSnd.get();
            futRec.get();
            assertEquals(tst2, rcv2);
        }

    }

    /**
     * Sends a large number of random buffers, confirming successful
     * transmission.
     * 
     * @throws TimeoutException
     * @throws ExecutionException
     * @throws InterruptedException
     */
    public void testStress() throws TimeoutException, InterruptedException,
            ExecutionException {

        for (int i = 0; i < 100; i++) {
            int sze = 10000 + r.nextInt(300000);
            final HAWriteMessage msg = new HAWriteMessage(sze, 0/*@todo chksum*/);
            final ByteBuffer tst = getRandomData(sze);
            final ByteBuffer rcv = ByteBuffer.allocate(sze);
            // FutureTask return ensures remote ready for Socket data
            final Future<Void> futRec = receiveService.receiveData(msg, rcv);
            final Future<Void> futSnd = sendService.send(tst);
            while (!futSnd.isDone() && !futRec.isDone()) {
                try {
                    futSnd.get(10L, TimeUnit.MILLISECONDS);
                } catch (TimeoutException ignored) {
                }
                try {
                    futRec.get(10L, TimeUnit.MILLISECONDS);
                } catch (TimeoutException ignored) {
                }
            }
            futSnd.get();
            futRec.get();
            assertEquals(tst, rcv); // make sure buffer has been transmitted
        }
    }

    /**
     * Sends a large number of random buffers, confirming successful
     * transmission.
     * 
     * @throws InterruptedException
     */
    public void testStressDirectBuffers() throws InterruptedException {

        ByteBuffer tst = null, rcv = null;
        int i = -1, sze = -1;
        try {
            tst = DirectBufferPool.INSTANCE.acquire();
            rcv = DirectBufferPool.INSTANCE.acquire();
            for (i = 0; i < 100; i++) {
                sze = 1 + r.nextInt(tst.capacity());
                final HAWriteMessage msg = new HAWriteMessage(sze, 0/*@todo chksum*/);
                getRandomData(tst, sze);
                assertEquals(0,tst.position());
                assertEquals(sze,tst.limit());
                // FutureTask return ensures remote ready for Socket data
                final Future<Void> futRec = receiveService.receiveData(msg, rcv);
                final Future<Void> futSnd = sendService.send(tst);
                while (!futSnd.isDone() && !futRec.isDone()) {
                    try {
                        futSnd.get(10L, TimeUnit.MILLISECONDS);
                    } catch (TimeoutException ignored) {
                    }
                    try {
                        futRec.get(10L, TimeUnit.MILLISECONDS);
                    } catch (TimeoutException ignored) {
                    }
                }
                futSnd.get();
                futRec.get();
                assertEquals(tst, rcv); // make sure buffer has been transmitted
            }
        } catch (Throwable t) {
            throw new RuntimeException("i=" + i + ": " + t, t);
        } finally {
            try {
                if (tst != null) {
                    DirectBufferPool.INSTANCE.release(tst);
                }
            } finally {
                if (rcv != null) {
                    DirectBufferPool.INSTANCE.release(rcv);
                }
            }
        }
    }
}
