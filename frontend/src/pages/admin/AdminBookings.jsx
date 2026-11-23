import { useEffect, useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { adminApi, errorMessage } from '../../api/client';
import { formatDateTime, formatPrice, statusBadge } from '../../utils';

export default function AdminBookings() {
  const [params, setParams] = useSearchParams();
  const showId = params.get('showId') || '';
  const [bookings, setBookings] = useState([]);
  const [status, setStatus] = useState('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const load = () => { setLoading(true); adminApi.bookings(showId).then(setBookings).catch((e) => setError(errorMessage(e))).finally(() => setLoading(false)); };
  useEffect(load, [showId]); // eslint-disable-line react-hooks/exhaustive-deps

  const cancel = async (b) => {
    if (!window.confirm(`Cancel booking ${b.reference}? Paid amount will be refunded.`)) return;
    try { await adminApi.cancelBooking(b.id); load(); } catch (e) { setError(errorMessage(e)); }
  };

  const shown = bookings.filter((b) => !status || b.status === status);
  const revenue = bookings.filter((b) => b.status === 'CONFIRMED').reduce((s, b) => s + Number(b.amount), 0);
  const tickets = bookings.filter((b) => b.status === 'CONFIRMED').reduce((s, b) => s + b.seats.length, 0);

  return (
    <>
      <div className="d-flex flex-wrap gap-2 justify-content-between align-items-center mb-3">
        <h3 className="mb-0">Bookings {showId && <small className="text-muted fs-6">for show #{showId} <Link to="/admin/bookings" onClick={() => setParams({})}>(all)</Link></small>}</h3>
        <select className="form-select w-auto" value={status} onChange={(e) => setStatus(e.target.value)}>
          <option value="">All statuses</option>
          {['CONFIRMED', 'PENDING_PAYMENT', 'CANCELLED', 'EXPIRED'].map((s) => <option key={s}>{s}</option>)}
        </select>
      </div>
      <div className="row g-3 mb-3">
        <div className="col-6 col-md-3"><div className="card border-0 shadow-sm p-3"><div className="small text-muted">Confirmed revenue</div><div className="fs-4 fw-bold">{formatPrice(revenue)}</div></div></div>
        <div className="col-6 col-md-3"><div className="card border-0 shadow-sm p-3"><div className="small text-muted">Tickets sold</div><div className="fs-4 fw-bold">{tickets}</div></div></div>
      </div>
      {error && <div className="alert alert-danger">{error}</div>}
      <div className="table-responsive card border-0 shadow-sm">
        <table className="table table-hover align-middle mb-0 small">
          <thead className="table-light"><tr><th>Booking</th><th>User</th><th>Movie / Show</th><th>Seats</th><th>Amount</th><th>Status</th><th>Booked on</th><th></th></tr></thead>
          <tbody>
            {loading && <tr><td colSpan="8" className="text-center py-4">Loading…</td></tr>}
            {!loading && shown.length === 0 && <tr><td colSpan="8" className="text-center py-4 text-muted">No bookings.</td></tr>}
            {shown.map((b) => (
              <tr key={b.id}>
                <td className="font-monospace"><Link to={`/bookings/${b.id}`}>{b.reference}</Link></td>
                <td>{b.userName}<div className="text-muted">{b.userEmail}</div></td>
                <td>{b.show.movieTitle}<div className="text-muted">{b.show.theaterName} · {formatDateTime(b.show.startTime)}</div></td>
                <td>{b.seats.join(', ')}</td>
                <td>{formatPrice(b.amount)}<div className="text-muted">{b.paymentStatus}</div></td>
                <td><span className={`badge bg-${statusBadge(b.status)}`}>{b.status.replace('_', ' ')}</span></td>
                <td className="text-nowrap">{formatDateTime(b.createdAt)}</td>
                <td>{['CONFIRMED', 'PENDING_PAYMENT'].includes(b.status) && new Date(b.show.startTime) > new Date() &&
                  <button className="btn btn-sm btn-outline-danger" onClick={() => cancel(b)}>Cancel</button>}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </>
  );
}
