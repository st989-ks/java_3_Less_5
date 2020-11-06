
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

public class MainClass {
    public static final int CARS_COUNT = 4;
    static Semaphore smp = new Semaphore( CARS_COUNT / 2 );
    static CyclicBarrier cb = new CyclicBarrier( CARS_COUNT + 1 );


    public static void main(String[] args) {
        System.out.println( "ВАЖНОЕ ОБЪЯВЛЕНИЕ >>> Подготовка!!!" );
        Race race = new Race( new Road( 60 ), new Tunnel(80),
                new Road( 40 ));
        Car[] cars = new Car[CARS_COUNT];
        for (int i = 0; i < cars.length; i++) {
            cars[i] = new Car( race, 20 + (int) (Math.random() * 10), cb );
        }
        for (Car car : cars) {
            new Thread( car ).start();
        }
        try {
            cb.await();
            System.out.println( "ВАЖНОЕ ОБЪЯВЛЕНИЕ >>> Гонка началась!!!" );
            cb.await();
            cb.await();
        } catch (InterruptedException | BrokenBarrierException e) {
            e.printStackTrace();
        }
        System.out.println( "ВАЖНОЕ ОБЪЯВЛЕНИЕ >>> Гонка закончилась!!!" );
    }
}


class Car implements Runnable {
    private static int CARS_COUNT;

    static {
        CARS_COUNT = 0;
    }

    private Race race;
    private int speed;
    private String name;

    private CyclicBarrier carBarrier;
    private static AtomicInteger ai = new AtomicInteger( 0 );

    public String getName() {
        return name;
    }

    public int getSpeed() {
        return speed;
    }

    public Car(Race race, int speed, CyclicBarrier carBarrier) {
        this.race = race;
        this.speed = speed;
        CARS_COUNT++;
        this.name = "Участник #" + CARS_COUNT;
        this.carBarrier = carBarrier;

    }

    @Override
    public void run() {
        try {
            System.out.println( this.name + " готовится" );
            Thread.sleep( 500 + (int) (Math.random() * 800) );
            System.out.println( this.name + " готов" );
            carBarrier.await();
            carBarrier.await();
            for (int i = 0; i < race.getStages().size(); i++) {
                race.getStages().get( i ).go( this );
            }
            if (ai.incrementAndGet() == 1) {
                System.out.println( name + " WIN" );
            }
            carBarrier.await();
        } catch (InterruptedException | BrokenBarrierException e) {
            e.printStackTrace();
        }
    }
}

abstract class Stage {
    protected int length;
    protected String description;

    public String getDescription() {
        return description;
    }

    public abstract void go(Car c);
}

class Road extends Stage {
    public Road(int length) {
        this.length = length;
        this.description = "Дорога " + length + " метров";
    }

    @Override
    public void go(Car c) {
        try {
            System.out.println( c.getName() + " готовится к этапу(ждет): " + description );
            System.out.println( c.getName() + " начал этап: " + description );
            Thread.sleep( length / c.getSpeed() * 1000 );
        } catch (InterruptedException e) {
            e.printStackTrace();
        }finally {
            System.out.println( c.getName() + " закончил этап: " + description );
        }
    }
}

class Tunnel extends Stage {
    public Tunnel(int length) {
        this.length = length;
        this.description = "Тоннель " + length + " метров";
    }

    @Override
    public void go(Car c) {
        try {
            try {
                System.out.println( c.getName() + " готовится к этапу(ждет): " + description );
                MainClass.smp.acquire();
                System.out.println( c.getName() + " начал этап: " + description );
                Thread.sleep( length / c.getSpeed() * 1000 );
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                System.out.println( c.getName() + " закончил этап: " + description );
                MainClass.smp.release();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

class Race {
    private ArrayList<Stage> stages;

    public ArrayList<Stage> getStages() {
        return stages;
    }

    public Race(Stage... stages) {
        this.stages = new ArrayList<>( Arrays.asList( stages ) );
    }
}

