/*
Test ID: TC-05
Test name: Payment Failure Simulation
Architecture: Monolith
Endpoint or flow: add to cart and checkout with simulatePaymentFailure=true
VUs: 5
Duration: 30s
*/
import { buildFailureOptions, uniqueCustomerId, fetchFirstProductId, addItemToCart, checkout } from '../shared.js';

const BASE_URL = __ENV.MONOLITH_BASE_URL || 'http://localhost:8083';
export const options = buildFailureOptions(5, '30s', 502);

export default function () {
  const customerId = uniqueCustomerId('mono-payment-failure');
  const productId = fetchFirstProductId(BASE_URL);
  addItemToCart(BASE_URL, customerId, productId, 1);
  checkout(BASE_URL, customerId, 'simulatePaymentFailure=true', 502, { scenario_type: 'failure' });
}
