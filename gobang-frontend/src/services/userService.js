import { API_URLS } from '@/config/apiConfig';

export const userLogin = (form) => {
  return fetch(API_URLS.USER_LOGIN, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(form),
  })
      .then((response) => {
        if (!response.ok) throw new Error(`HTTP错误: ${response.status}`); // 新增错误检查
        return response.json();
      });
};

export const userRegister = (form) => {
  return fetch(API_URLS.USER_REGISTER, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify(form),
  })
      .then((response) => response.json());
};

export const fetchUserInfo = () => {
  const token = localStorage.getItem('token');
  const userId = localStorage.getItem('userId');
  return fetch(`${API_URLS.USER_INFO}?userId=${userId}`, {
    method: 'GET',
    headers: {
      'Authorization': token ? 'Bearer ' + token : ''
    }
  }).then(res => res.json());
};
