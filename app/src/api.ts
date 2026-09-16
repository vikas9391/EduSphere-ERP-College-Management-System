import AsyncStorage from '@react-native-async-storage/async-storage';

const API_URL = process.env.EXPO_PUBLIC_API_URL ?? 'http://localhost:8080/api';

export type LoginResponse = {
  accessToken: string;
  refreshToken?: string;
  role?: string;
  username?: string;
  mustChangePassword?: boolean;
};

export async function login(collegeCode: string, username: string, password: string) {
  const response = await fetch(`${API_URL}/auth/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ collegeCode, username, password }),
  });

  const data = await response.json();
  if (!response.ok) throw new Error(data?.message ?? 'Login failed');

  const result = (data?.data ?? data) as LoginResponse;
  await AsyncStorage.setItem('accessToken', result.accessToken);
  if (result.refreshToken) await AsyncStorage.setItem('refreshToken', result.refreshToken);
  await AsyncStorage.setItem('userRole', result.role ?? '');
  return result;
}

export async function logout() {
  await AsyncStorage.multiRemove(['accessToken', 'refreshToken', 'userRole']);
}

export async function getAccessToken() {
  return AsyncStorage.getItem('accessToken');
}
