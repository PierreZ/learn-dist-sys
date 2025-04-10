///usr/bin/env jbang "$0" "$@" ; exit $?

public class VirtualThreadDemo {

  public static void main(String... args) throws InterruptedException {
    Thread.ofVirtual().start(() -> {

      System.out.println("toto");
      System.out.println(Thread.currentThread());
    });

    // Platform (or OS) thread
    Thread.ofPlatform().start(() -> {
      System.out.println(Thread.currentThread());
    });

    // Virtual thread
    Thread.ofVirtual().start(() -> {
      System.out.println(Thread.currentThread());
    });

    Thread.sleep(1000);
  }
}
