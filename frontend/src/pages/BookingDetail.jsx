import { useEffect, useState } from 'react';
import { Link, useLocation, useParams } from 'react-router-dom';
import { bookingApi, errorMessage } from '../api/client';
import { formatDateTime, formatPrice, statusBadge } from '../utils';

export default function BookingDetail() {
  const { id } = useParams();
  const location = useLocation();
  const [b, setB] = useState(null);
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);

  useEffect(() => { bookingApi.get(id).then(setB).catch((e) => setError(errorMessage(e))); }, [id]);

  const cancel = async () => {
    if (!window.confirm('Cancel this booking? Paid amount will be refunded.')) return;
    setBusy(true);
    try { setB(await bookingApi.cancel(id)); } catch (e) { setError(errorMessage(e)); } finally { setBusy(false); }
  };

  if (error && !b) return <div className="alert alert-danger">{error}</div>;
  if (!b) return <div className="text-center py-5">Loading…</div>;
  const s = b.show;

  return (
    <div className="row justify-content-center">
      <div className="col-lg-6">
        {location.state?.justBooked && b.status === 'CONFIRMED' && (
          <div className="alert alert-success">🎉 Booking confirmed! A confirmation email has been sent to {b.userEmail}.</div>
        )}
        {error && <div className="alert alert-danger">{error}</div>}
        <div className="ticket p-4 shadow-sm">
          <div className="d-flex justify-content-between align-items-start">
            <div>
              <div className="small text-muted">CineBook ticket</div>
              <h4 className="mb-0">{s.movieTitle}</h4>
              <div className="small text-muted">{s.certificate} · {s.language}</div>
            </div>
            <span className={`badge bg-${statusBadge(b.status)}`}>{b.status.replace('_', ' ')}</span>
          </div>
          <hr />
          <div className="row g-3 small">
            <div className="col-6"><div className="text-muted">Date & time</div><strong>{formatDateTime(s.startTime)}</strong></div>
            <div className="col-6"><div className="text-muted">Theater</div><strong>{s.theaterName}</strong><div>{s.address || s.city}</div></div>
            <div className="col-6"><div className="text-muted">Seats</div><strong className="fs-5">{b.seats.join(', ')}</strong></div>
            <div className="col-6"><div className="text-muted">Amount</div><strong className="fs-5">{formatPrice(b.amount)}</strong></div>
            <div className="col-6"><div className="text-muted">Payment</div><strong>{b.paymentStatus}</strong>{b.paymentId && <div className="text-muted">{b.paymentId}</div>}</div>
            <div className="col-6"><div className="text-muted">Booking ID</div><strong className="fs-4 font-monospace">{b.reference}</strong></div>
          </div>
          {b.cancelledAt && <p className="small text-muted mt-3 mb-0">Cancelled on {formatDateTime(b.cancelledAt)}{b.paymentStatus === 'REFUNDED' && ' · refund initiated'}</p>}
        </div>
        <div className="d-flex gap-2 mt-3">
          <Link to="/bookings" className="btn btn-outline-secondary">My bookings</Link>
          {b.status === 'PENDING_PAYMENT' && <Link to={`/checkout/${b.id}`} className="btn btn-brand">Complete payment</Link>}
          {b.status === 'CONFIRMED' && b.cancellable && <button className="btn btn-outline-danger ms-auto" disabled={busy} onClick={cancel}>Cancel booking</button>}
        </div>
      </div>
    </div>
  );
}
