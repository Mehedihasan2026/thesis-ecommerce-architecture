/*
Test ID: TC-07
Test name: Payment Delay Simulation
Architecture: Monolith
Endpoint or flow: add to cart and checkout with artificialPaymentDelayMs=1000
VUs: 5
Duration: 30s
*/
import { buildOptions, uniqueCustomerId, fetchFirstProductId, addItemToCart, checkout } from '../shared.js';

const BASE_URL = __ENV.MONOLITH_BASE_URL || 'http://localhost:8083';
export const options = buildOptions(5, '30s', {
  http_req_duration: ['p(95)<4000', 'p(99)<5000'],
});

export default function () {
  const customerId = uniqueCustomerId('mono-payment-delay');
  const productId = fetchFirstProductId(BASE_URL);
  addItemToCart(BASE_URL, customerId, productId, 1);
  checkout(BASE_URL, customerId, 'artificialPaymentDelayMs=1000', 200, { scenario_type: 'delay' });
}
