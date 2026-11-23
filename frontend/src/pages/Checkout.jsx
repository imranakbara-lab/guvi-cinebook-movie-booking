import { useEffect, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { bookingApi, errorMessage } from '../api/client';
import { formatDateTime, formatPrice, formatSeconds } from '../utils';

function loadRazorpay() {
  return new Promise((resolve) => {
    if (window.Razorpay) return resolve(true);
    const s = document.createElement('script');
    s.src = 'https://checkout.razorpay.com/v1/checkout.js';
    s.onload = () => resolve(true);
    s.onerror = () => resolve(false);
    document.body.appendChild(s);
  });
}

export default function Checkout() {
  const { bookingId } = useParams();
  const navigate = useNavigate();
  const [booking, setBooking] = useState(null);
  const [left, setLeft] = useState(0);
  const [method, setMethod] = useState('UPI');
  const [paying, setPaying] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    bookingApi.get(bookingId).then((b) => {
      if (b.status === 'CONFIRMED') navigate(`/bookings/${b.id}`, { replace: true });
      setBooking(b);
      setLeft(b.holdSecondsLeft);
    }).catch((e) => setError(errorMessage(e)));
  }, [bookingId, navigate]);

  useEffect(() => {
    if (!booking || booking.status !== 'PENDING_PAYMENT') return undefined;
    const end = Date.now() + booking.holdSecondsLeft * 1000;
    const t = setInterval(() => setLeft(Math.max(0, Math.round((end - Date.now()) / 1000))), 1000);
    return () => clearInterval(t);
  }, [booking]);

  const done = (b) => navigate(`/bookings/${b.id}`, { replace: true, state: { justBooked: true } });

  const pay = async () => {
    setPaying(true);
    setError('');
    try {
      const order = await bookingApi.paymentOrder(booking.id);
      if (order.provider === 'RAZORPAY') {
        if (!(await loadRazorpay())) throw new Error('Could not load Razorpay checkout');
        const rzp = new window.Razorpay({
          key: order.keyId,
          order_id: order.orderId,
          amount: order.amountInPaise,
          currency: order.currency,
          name: 'CineBook',
          description: order.description,
          prefill: { name: order.customerName, email: order.customerEmail },
          theme: { color: '#e50914' },
          handler: async (resp) => {
            try {
              done(await bookingApi.confirm(booking.id, {
                razorpayOrderId: resp.razorpay_order_id,
                razorpayPaymentId: resp.razorpay_payment_id,
                razorpaySignature: resp.razorpay_signature,
              }));
            } catch (e) { setError(errorMessage(e)); setPaying(false); }
          },
          modal: { ondismiss: () => setPaying(false) },
        });
        rzp.open();
      } else {
        // Test mode: no gateway keys configured on the server - payment is simulated
        await new Promise((r) => setTimeout(r, 1200));
        done(await bookingApi.confirm(booking.id));
      }
    } catch (e) {
      setError(errorMessage(e));
      setPaying(false);
    }
  };

  const release = async () => {
    if (!window.confirm('Release these seats and go back?')) return;
    try { await bookingApi.cancel(booking.id); } catch { /* ignore */ }
    navigate(`/shows/${booking.show.id}/seats`, { replace: true });
  };

  if (error && !booking) return <div className="alert alert-danger">{error}</div>;
  if (!booking) return <div className="text-center py-5">Loading…</div>;

  const s = booking.show;
  const expired = booking.status === 'EXPIRED' || (booking.status === 'PENDING_PAYMENT' && left === 0);

  return (
    <div className="row justify-content-center">
      <div className="col-lg-7">
        <div className="card border-0 shadow-sm">
          <div className="card-header bg-dark text-white d-flex justify-content-between align-items-center">
            <span className="fw-semibold">Checkout</span>
            {booking.status === 'PENDING_PAYMENT' && <span className={`hold-timer ${left < 60 ? 'text-warning' : ''}`}>Seats held for {formatSeconds(left)}</span>}
          </div>
          <div className="card-body p-4">
            <h4 className="mb-1">{s.movieTitle}</h4>
            <p className="text-muted mb-3">{s.theaterName}, {s.city} · {formatDateTime(s.startTime)}</p>
            <table className="table table-sm mb-3">
              <tbody>
                <tr><th>Seats</th><td>{booking.seats.join(', ')}</td></tr>
                <tr><th>Tickets</th><td>{booking.seats.length}</td></tr>
                <tr><th>Booking ID</th><td>{booking.reference}</td></tr>
                <tr className="fs-5"><th>Total</th><td className="fw-bold">{formatPrice(booking.amount)}</td></tr>
              </tbody>
            </table>

            {expired ? (
              <div className="alert alert-warning">Your seat hold expired. <Link to={`/shows/${s.id}/seats`}>Select seats again</Link></div>
            ) : booking.status !== 'PENDING_PAYMENT' ? (
              <div className="alert alert-secondary">This booking is {booking.status.toLowerCase()}.</div>
            ) : (
              <>
                <h6>Payment method</h6>
                <div className="d-flex gap-2 mb-3">
                  {['UPI', 'Card', 'Net Banking'].map((m) => (
                    <button key={m} type="button" className={`btn btn-sm ${method === m ? 'btn-dark' : 'btn-outline-secondary'}`} onClick={() => setMethod(m)}>{m}</button>
                  ))}
                </div>
                {error && <div className="alert alert-danger">{error}</div>}
                <button className="btn btn-brand btn-lg w-100" disabled={paying} onClick={pay}>
                  {paying ? 'Processing payment…' : `Pay ${formatPrice(booking.amount)}`}
                </button>
                <p className="small text-muted mt-2 mb-2">Payments go through Razorpay when configured; otherwise the app runs in test mode and the payment is simulated – no money is charged.</p>
                <button className="btn btn-link btn-sm px-0" onClick={release}>Cancel and release seats</button>
              </>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
