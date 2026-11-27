export async function login(email, password) {
  const response = await fetch(`${AUTH_URL}/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email, password }),
  });

  if (!response.ok) {
    throw new Error('Wrong email or password');
  }

  const data = await response.json();
  localStorage.setItem('accessToken', data.accessToken);

  const name = email.split('@')[0];
  localStorage.setItem('displayName', name.charAt(0).toUpperCase() + name.slice(1));

  return data;
}