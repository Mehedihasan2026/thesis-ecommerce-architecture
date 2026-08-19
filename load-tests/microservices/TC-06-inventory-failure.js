/*
Test ID: TC-06
Test name: Inventory Failure Simulation
Architecture: Microservices
Endpoint or flow: catalog -> cart -> order checkout with simulateInventoryFailure=true
VUs: 5
Duration: 30s
*/
import { buildFailureOptions, uniqueCustomerId, fetchFirstProductId, addItemToCart, checkout } from '../shared.js';

const CATALOG_BASE_URL = __ENV.CATALOG_BASE_URL || 'http://localhost:8085';
const CART_BASE_URL = __ENV.CART_BASE_URL || 'http://localhost:8086';
const ORDER_BASE_URL = __ENV.ORDER_BASE_URL || 'http://localhost:8089';
export const options = buildFailureOptions(5, '30s', 409);

export default function () {
  const customerId = uniqueCustomerId('micro-inventory-failure');
  const productId = fetchFirstProductId(CATALOG_BASE_URL);
  addItemToCart(CART_BASE_URL, customerId, productId, 1);
  checkout(ORDER_BASE_URL, customerId, 'simulateInventoryFailure=true', 409, { scenario_type: 'failure' });
}
