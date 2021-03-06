/*
 * Copyright (c) 2015 Red Hat, Inc. and/or its affiliates.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Cheng Fang - Initial API and implementation
 */

package org.jberet.testapps.chunkskipretry;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.batch.runtime.BatchStatus;

import org.jberet.testapps.common.AbstractIT;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests to verify skip and retry behaviors by configuring {@code ArithmeticException} as skippable-exception,
 * retryable-exception, and both skippable-exception and retryable-exception.
 * <p/>
 * Tests compare the content and structure of written result to an expected values. Note that these comparison may be
 * too restrictive and may need to change as the jberet-core changes.
 */
public class ChunkSkipRetryIT extends AbstractIT {
    protected int dataCount = 30;
    protected static final String chunkRetryXml = "chunkRetry.xml";
    protected static final String chunkSkipXml = "chunkSkip.xml";
    protected static final String chunkSkipRetryXml = "chunkSkipRetry.xml";

    @Before
    public void before() throws Exception {
        super.before();
        params.setProperty("data.count", String.valueOf(dataCount));
    }

    @After
    public void after() {
        params.clear();
    }

    @Test
    public void retryRead() throws Exception {
        params.setProperty("reader.fail.on.values", "5");
        startJob(chunkRetryXml);
        awaitTermination();
        Assert.assertEquals(BatchStatus.COMPLETED, jobExecution.getBatchStatus());

        final ArrayList<List<Integer>> expected = new ArrayList<List<Integer>>();
        expected.add(Arrays.asList(0));
        expected.add(Arrays.asList(1));
        expected.add(Arrays.asList(2));
        expected.add(Arrays.asList(3));
        expected.add(Arrays.asList(4));
        expected.add(Arrays.asList(5));
        expected.add(Arrays.asList(6, 7, 8, 9, 10, 11, 12, 13, 14, 15));
        expected.add(Arrays.asList(16, 17, 18, 19, 20, 21, 22, 23, 24, 25));
        expected.add(Arrays.asList(26, 27, 28, 29));

        checkRecordedData(expected);
    }

    @Test
    public void retryWrite() throws Exception {
        params.setProperty("writer.fail.on.values", "5");
        startJob(chunkRetryXml);
        awaitTermination();
        Assert.assertEquals(BatchStatus.COMPLETED, jobExecution.getBatchStatus());

        final ArrayList<List<Integer>> expected = new ArrayList<List<Integer>>();
        expected.add(Arrays.asList(0));
        expected.add(Arrays.asList(1));
        expected.add(Arrays.asList(2));
        expected.add(Arrays.asList(3));
        expected.add(Arrays.asList(4));
        expected.add(Arrays.asList(5));
        expected.add(Arrays.asList(6));
        expected.add(Arrays.asList(7));
        expected.add(Arrays.asList(8));
        expected.add(Arrays.asList(9));
        expected.add(Arrays.asList(10));
        expected.add(Arrays.asList(11, 12, 13, 14, 15, 16, 17, 18, 19, 20));
        expected.add(Arrays.asList(21, 22, 23, 24, 25, 26, 27, 28, 29));

        checkRecordedData(expected);
    }

    @Test
    public void retryProcess() throws Exception {
        params.setProperty("processor.fail.on.values", "5");
        startJob(chunkRetryXml);
        awaitTermination();
        Assert.assertEquals(BatchStatus.COMPLETED, jobExecution.getBatchStatus());

        final ArrayList<List<Integer>> expected = new ArrayList<List<Integer>>();
        expected.add(Arrays.asList(0));
        expected.add(Arrays.asList(1));
        expected.add(Arrays.asList(2));
        expected.add(Arrays.asList(3));
        expected.add(Arrays.asList(4));
        expected.add(Arrays.asList(5));
        expected.add(Arrays.asList(6));
        expected.add(Arrays.asList(7, 8, 9, 10, 11, 12, 13, 14, 15, 16));
        expected.add(Arrays.asList(17, 18, 19, 20, 21, 22, 23, 24, 25, 26));
        expected.add(Arrays.asList(27, 28, 29));

        //although the processor failed at 5, retry in item processor will retry up to 6 (one extra one-item chunk)
        //In org.jberet.runtime.runner.ChunkRunner.checkIfEndRetry()
        //processingInfo.readPosition - 1 == processingInfo.failurePoint
        //
        //when failed in item processor, the recorded failurePoint (assigned from current readPosition, which has
        //already been advanced to the next slot) was 6, instead of 5

        checkRecordedData(expected);
    }

    @Test
    public void skipRead() throws Exception {
        params.setProperty("reader.fail.on.values", "5");
        startJob(chunkSkipXml);
        awaitTermination();
        Assert.assertEquals(BatchStatus.COMPLETED, jobExecution.getBatchStatus());

        final ArrayList<List<Integer>> expected = new ArrayList<List<Integer>>();
        expected.add(Arrays.asList(0, 1, 2, 3, 4, 6, 7, 8, 9, 10));  //skip 5
        expected.add(Arrays.asList(11, 12, 13, 14, 15, 16, 17, 18, 19, 20));
        expected.add(Arrays.asList(21, 22, 23, 24, 25, 26, 27, 28, 29));

        checkRecordedData(expected);
    }

    @Test
    public void skipWrite() throws Exception {
        params.setProperty("writer.fail.on.values", "5");
        startJob(chunkSkipXml);
        awaitTermination();
        Assert.assertEquals(BatchStatus.COMPLETED, jobExecution.getBatchStatus());

        final ArrayList<List<Integer>> expected = new ArrayList<List<Integer>>();
        //skip the chunk where 5 is located (0-9)
        expected.add(Arrays.asList(10, 11, 12, 13, 14, 15, 16, 17, 18, 19));
        expected.add(Arrays.asList(20, 21, 22, 23, 24, 25, 26, 27, 28, 29));

        checkRecordedData(expected);
    }

    @Test
    public void skipProcess() throws Exception {
        params.setProperty("processor.fail.on.values", "5");
        startJob(chunkSkipXml);
        awaitTermination();
        Assert.assertEquals(BatchStatus.COMPLETED, jobExecution.getBatchStatus());

        final ArrayList<List<Integer>> expected = new ArrayList<List<Integer>>();
        expected.add(Arrays.asList(0, 1, 2, 3, 4, 6, 7, 8, 9));  //skip 5
        expected.add(Arrays.asList(10, 11, 12, 13, 14, 15, 16, 17, 18, 19));
        expected.add(Arrays.asList(20, 21, 22, 23, 24, 25, 26, 27, 28, 29));

        checkRecordedData(expected);
    }

    @Test
    public void retrySkipRead() throws Exception {
        params.setProperty("reader.fail.on.values", "5");
        params.setProperty("repeat.failure", "true");
        startJob(chunkSkipRetryXml);
        awaitTermination();
        Assert.assertEquals(BatchStatus.COMPLETED, jobExecution.getBatchStatus());

        final ArrayList<List<Integer>> expected = new ArrayList<List<Integer>>();
        expected.add(Arrays.asList(0));
        expected.add(Arrays.asList(1));
        expected.add(Arrays.asList(2));
        expected.add(Arrays.asList(3));
        expected.add(Arrays.asList(4));
        //expected.add(Arrays.asList(5));  //failed, re-read and failed and skipped
        expected.add(Arrays.asList(6, 7, 8, 9, 10, 11, 12, 13, 14, 15));
        expected.add(Arrays.asList(16, 17, 18, 19, 20, 21, 22, 23, 24, 25));
        expected.add(Arrays.asList(26, 27, 28, 29));

        checkRecordedData(expected);
    }

    @Test
    public void retrySkipWrite() throws Exception {
        params.setProperty("writer.fail.on.values", "5");
        params.setProperty("repeat.failure", "true");
        startJob(chunkSkipRetryXml);
        awaitTermination();
        Assert.assertEquals(BatchStatus.COMPLETED, jobExecution.getBatchStatus());

        final ArrayList<List<Integer>> expected = new ArrayList<List<Integer>>();
        expected.add(Arrays.asList(0));
        expected.add(Arrays.asList(1));
        expected.add(Arrays.asList(2));
        expected.add(Arrays.asList(3));
        expected.add(Arrays.asList(4));
        //expected.add(Arrays.asList(5));  //failed, re-written and failed and skipped
        expected.add(Arrays.asList(6));
        expected.add(Arrays.asList(7));
        expected.add(Arrays.asList(8));
        expected.add(Arrays.asList(9));
        expected.add(Arrays.asList(10));
        expected.add(Arrays.asList(11, 12, 13, 14, 15, 16, 17, 18, 19, 20));
        expected.add(Arrays.asList(21, 22, 23, 24, 25, 26, 27, 28, 29));

        //see comments in test retryProcess why [6] is retried in its own one-item chunk

        checkRecordedData(expected);
    }

    @Test
    public void retrySkipProcess() throws Exception {
        params.setProperty("processor.fail.on.values", "5");
        params.setProperty("repeat.failure", "true");
        startJob(chunkSkipRetryXml);
        awaitTermination();
        Assert.assertEquals(BatchStatus.COMPLETED, jobExecution.getBatchStatus());

        final ArrayList<List<Integer>> expected = new ArrayList<List<Integer>>();
        expected.add(Arrays.asList(0));
        expected.add(Arrays.asList(1));
        expected.add(Arrays.asList(2));
        expected.add(Arrays.asList(3));
        expected.add(Arrays.asList(4));
        //expected.add(Arrays.asList(5));  //failed, re-read and failed and skipped
        expected.add(Arrays.asList(6));
        expected.add(Arrays.asList(7, 8, 9, 10, 11, 12, 13, 14, 15, 16));
        expected.add(Arrays.asList(17, 18, 19, 20, 21, 22, 23, 24, 25, 26));
        expected.add(Arrays.asList(27, 28, 29));

        checkRecordedData(expected);
    }

    private void checkRecordedData(final ArrayList<List<Integer>> expected) {
        final ArrayList<List<Object>> recorded = (ArrayList<List<Object>>) stepExecution0.getPersistentUserData();
        Assert.assertEquals(expected, recorded);
    }
}
