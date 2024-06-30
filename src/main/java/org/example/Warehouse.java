package org.example;

import lombok.Getter;
import lombok.extern.log4j.Log4j2;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

@Log4j2
@Getter
public class Warehouse extends Thread {

    private final List<Block> storage = new ArrayList<>();
    private final BlockingQueue<Truck> arrivedTrucks = new LinkedBlockingQueue<>();

    public Warehouse(String name) {
        super(name);
    }

    public Warehouse(String name, Collection<Block> initialStorage) {
        this(name);
        storage.addAll(initialStorage);
    }

    @Override
    public void run() {
        Truck truck;
        while (!currentThread().isInterrupted()) {
            try {
                truck = getNextArrivedTruck();
            } catch (InterruptedException e) {
                break;
            }
            if (truck.getBlocks().isEmpty()) {
                loadTruck(truck);
            } else {
                unloadTruck(truck);
            }
        }
        log.info("Warehouse thread interrupted");

    }

    private void loadTruck(Truck truck) {
        log.info("Loading truck {}", truck.getName());
        Collection<Block> blocksToLoad = getFreeBlocks(truck.getCapacity());
        try {
            sleep(10L * blocksToLoad.size());
        } catch (InterruptedException e) {
            log.error("Interrupted while loading truck", e);
        }
        truck.getBlocks().addAll(blocksToLoad);
        log.info("Truck loaded {}", truck.getName());
        synchronized (truck) {
            truck.setReady(true);
            truck.notifyAll();
        }
    }

    private synchronized Collection<Block> getFreeBlocks(int maxItems) {
        List<Block> blocks = new ArrayList<>();
        for (int i = 0; i < maxItems; i++) {
            if (storage.isEmpty()) {
                log.info("no more blocks");
                break;
            }
            blocks.add(storage.remove(0));
        }
        return blocks;
    }

    private synchronized void returnBlocksToStorage(List<Block> returnedBlocks) {
        storage.addAll(returnedBlocks);
    }

    private void unloadTruck(Truck truck) {
        log.info("Unloading truck {}", truck.getName());
        List<Block> arrivedBlocks = truck.getBlocks();
        try {
            sleep(10L * arrivedBlocks.size());
        } catch (InterruptedException e) {
            log.error("Interrupted while unloading truck", e);
        }
        returnBlocksToStorage(arrivedBlocks);
        truck.getBlocks().clear();
        synchronized (truck) {
            truck.setReady(true);
            truck.notifyAll();
        }
        log.info("Truck unloaded {}", truck.getName());
    }

    private Truck getNextArrivedTruck() throws InterruptedException {
        return arrivedTrucks.take();
    }


    public void arrive(Truck truck) {
        try {
            arrivedTrucks.put(truck);
            synchronized (truck) {
                while (!truck.isReady()) {
                    truck.wait();
                }
            }
            log.info("leaving {}", truck.getName());
        } catch (InterruptedException e) {
            log.info("truck arrived interrupted");
        }
    }
}
