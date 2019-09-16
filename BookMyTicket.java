package com.pp.bookmyshow;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.locks.ReentrantLock;

class BookingResult {
	String msg;
	boolean isBooked;

	public String getMsg() {
		return msg;
	}

	public void setMsg(String msg) {
		this.msg = msg;
	}

	public boolean isBooked() {
		return isBooked;
	}

	public void setBooked(boolean isBooked) {
		this.isBooked = isBooked;
	}

	public BookingResult(String msg, boolean isBooked) {
		super();
		this.msg = msg;
		this.isBooked = isBooked;
	}

	public BookingResult() {
	}

}

class User {

	private String name;
	private String mobile;

	public User(String name, String mobile) {
		super();
		this.name = name;
		this.mobile = mobile;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getMobile() {
		return mobile;
	}

	public void setMobile(String mobile) {
		this.mobile = mobile;
	}

}

public class BookMyTicket implements Callable<BookingResult> {
	protected static volatile Map<Integer, User> seatsMap = new HashMap<Integer, User>();
	ReentrantLock lock = new ReentrantLock();
	int seatNum;

	public BookMyTicket(int seatNum, User user) {
		super();
		this.seatNum = seatNum;
		this.user = user;
	}

	User user;

	static ExecutorService executor = null;
	static {
		executor = Executors.newCachedThreadPool();

		seatsMap.put(1, null);
		seatsMap.put(2, null);
		seatsMap.put(3, null);
		seatsMap.put(4, null);
		seatsMap.put(5, null);
	}

	protected boolean isSeatAvailable(int seatNum, User user) {
		try {
			System.out.println(
					"Thread " + Thread.currentThread().getName() + "  looking to acquire lock for seat No:" + seatNum);
			lock.lock();
			System.out.println("Thread " + Thread.currentThread().getName() + "  acquired lock for seat No:" + seatNum);
			return checkSeatAvailable(seatNum, user);
		} finally {
			lock.unlock();
		}
		// return seatBookingMSG;

	}

	protected boolean checkSeatAvailable(int seatNum, User user) {
		boolean flag = false;
		if (seatsMap.containsKey(seatNum) && seatsMap.get(seatNum) == null) {
			// updating into db that user has acquired the seat
			seatsMap.put(seatNum, user);
			System.out.println(" seat number:" + seatNum + " is allocated  to user " + user.getName() + "by Thraed :"
					+ Thread.currentThread().getName());
			flag = true;
		}
		return flag;
	}

	public boolean bookTicket(int seatNum, User user) {

		return true;
	}

	public BookingResult bookOurShow(int seatNum, User user) throws InterruptedException, ExecutionException {
		String seatBookingMSG = " Seat booking is in progress";
		BookingResult bkResult = new BookingResult();
		bkResult.setBooked(false);
		bkResult.setMsg(" Oops seat is already booked.Please try another seat.");
		Future<BookingResult> result = null;
		boolean seatAvailable = isSeatAvailable(seatNum, user);
		if (!seatAvailable) {
			System.out.println("if Thread " + Thread.currentThread().getName() + bkResult.getMsg());
			return bkResult;
		} else {
			// Need to make asynchronous task i.e Completable future
			result = executor.submit(new BookTicket(seatNum, user));
			System.out.println("else Thread " + Thread.currentThread().getName() + result.get().getMsg());
			return result.get();
		}
	}

	public static void main(String[] args) {
		ExecutorService service = Executors.newFixedThreadPool(10);
		for (int i = 0; i < 5; i++) {
			if (i == 2) {
				Future<BookingResult> submit = service
						.submit(new BookMyTicket(1, new User("User :" + (i + 1), "9920xxxx1" + (i + 1))));
			} else {
				Future<BookingResult> submit = service
						.submit(new BookMyTicket(i + 1, new User("User :" + (i + 1), "9920xxxx1" + (i + 1))));
			}
		}
	}

	@Override
	public BookingResult call() throws Exception {
		return bookOurShow(seatNum, user);
	}

}

class BookTicket implements Callable<BookingResult> {
	String msg = "Seat booked successfully :).";
	BookingResult bkResult = new BookingResult();
	int seatNum;
	User user;

	public boolean doPayment(double amount) {
		// microservice payment gatway call.
		return true;
	}

	public BookTicket(int seatNum, User user) {
		super();
		this.seatNum = seatNum;
		this.user = user;
	}

	// @Transaction
	@Override
	public BookingResult call() throws Exception {
		ReentrantLock bkTicketLock = new ReentrantLock();
		try {
			bkTicketLock.lock();

			bkResult.setMsg(msg);
			bkResult.setBooked(true);

			if (!doPayment(400)) {
				msg = "Seat booking failed, because of something went wrong.. :(";

				bkResult.setMsg(msg);
				bkResult.setBooked(false);
				// if it is connected with db then update in db.
			}
		} finally {
			bkTicketLock.unlock();
		}
		return bkResult;
	}
}
