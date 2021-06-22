package com.example.myapplicationdfsd.softWareSystem.service.media.data;

import java.util.concurrent.LinkedBlockingQueue;

public class MediaDataQueue<T> {

    private int mQueueMaxCount = 100;

    private LinkedBlockingQueue<T> queue;

    public MediaDataQueue() {
        this.queue = new LinkedBlockingQueue<>(mQueueMaxCount);
    }

    public MediaDataQueue(int mQueueMaxCount) {
        if(mQueueMaxCount>0){
            this.mQueueMaxCount = mQueueMaxCount;
            this.queue = new LinkedBlockingQueue<>(mQueueMaxCount);
        }else {
            throw new IllegalArgumentException("mQueueMaxCount has to be greater than 0");
        }

    }
    public boolean checkQueueSizeLowerThen0(){
        if (queue.size() < 1) {
            try {
                Thread.sleep(5);
                return true;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }
        return false;
    }

    public boolean offer(T t){
        return queue.offer(t);
    }


    public T poll(){
        return queue.poll();
    }
}
