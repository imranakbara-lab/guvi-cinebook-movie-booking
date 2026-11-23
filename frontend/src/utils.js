export const formatPrice = (v) =>
  new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', maximumFractionDigits: 0 }).format(Number(v || 0));
export const formatDate = (v) => (v ? new Date(v).toLocaleDateString('en-IN', { weekday: 'short', day: '2-digit', month: 'short', year: 'numeric' }) : '');
export const formatTime = (v) => (v ? new Date(v).toLocaleTimeString('en-IN', { hour: '2-digit', minute: '2-digit' }) : '');
export const formatDateTime = (v) => (v ? `${formatDate(v)}, ${formatTime(v)}` : '');
export const isoDate = (d) => {
  const z = new Date(d.getTime() - d.getTimezoneOffset() * 60000);
  return z.toISOString().slice(0, 10);
};
export const nextDays = (n) => Array.from({ length: n }, (_, i) => { const d = new Date(); d.setDate(d.getDate() + i); return d; });
export const formatSeconds = (s) => `${String(Math.floor(s / 60)).padStart(2, '0')}:${String(s % 60).padStart(2, '0')}`;
export const statusBadge = (s) => ({ CONFIRMED: 'success', PENDING_PAYMENT: 'warning text-dark', CANCELLED: 'secondary', EXPIRED: 'light text-dark' }[s] || 'secondary');
export const POSTER_FALLBACK = 'data:image/svg+xml;utf8,' + encodeURIComponent('<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 200 300"><rect width="200" height="300" fill="#2b2b3a"/><text x="100" y="160" font-size="48" text-anchor="middle" fill="#888">🎬</text></svg>');
