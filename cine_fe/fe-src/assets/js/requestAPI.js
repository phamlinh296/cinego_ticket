// const localIP = ''
const localIP = 'http://localhost:9595'

const GetIP = function () {
  return localIP
}


const PenguRequestAPI = async function (method = 'GET', path = 'api', _opti = {}, _hds = {}, usingToken = false) {
  const ip = GetIP()
  const url = `${ip}/${path}`

  let headers = new Headers();
  for (let key in _hds)
    headers.append(key, _hds[key]);

  let options = {
    method: method,
    headers: headers
  }

  for (let key in _opti)
    options[key] = _opti[key]

  if (options.body)
    options.body = JSON.stringify(options.body);

  if (usingToken) {
    $('.preloader').fadeOut(100);

    if (! await TokenIsVaild()) {
      await Swal.fire({
        icon: 'error',
        title: 'Thông báo',
        text: 'Vui lòng đăng nhập trước khi thao tác',
      })
      // window.location.href = ('sign-in.html?redirect=' + encodeURIComponent(window.location.href))

      // await sleep(100000); // Make sure for redirected
      // return false;

      //lưu lại trang trước đó để quay về sau khi đăng nhập.
      const currentUrl = window.location.href;
      window.location.href = `sign-in.html?redirect=${encodeURIComponent(currentUrl)}`;

      //check token xem lỗi k
      console.log("Token hiện tại:", GetToken());
      console.log("Lần kiểm tra trước:", localStorage.LAST_CHECK_TOKEN);

      return false;

    }


    headers.append('Authorization', 'Bearer ' + GetToken());
  }
  // 
  // const baseUrl = "http://localhost:9595"
  // let response = await fetch(baseUrl + url, options)
  let response = await fetch(GetIP() + "/" + path, options);
  console.log("url====" + GetIP() + "/" + path, options)// check

  if (response.status != 200) localStorage.LAST_CHECK_TOKEN = 0

  return response
}


const TokenCookieName = "pengu_token"

const GetToken = function () {
  return $.cookie(TokenCookieName)
}

const SetToken = function (token) {
  $.cookie(TokenCookieName, token)
}

const RemoveToken = function () {
  $.removeCookie(TokenCookieName);
}

const TimeCheckAgainToken = 60 * 1000

const TokenIsVaild = async function () {//hàm token này
  if (GetToken() == undefined) return false

  let lastCheckToken = localStorage.LAST_CHECK_TOKEN || 0
  localStorage.LAST_CHECK_TOKEN = Date.now()

  if (Date.now() - lastCheckToken > TimeCheckAgainToken) {
    // Check Expire of token
    let headers = new Headers();
    headers.append('Authorization', 'Bearer ' + GetToken());

    let options = {
      method: 'GET',
      headers: headers
    }

    const ip = GetIP()
    let res = await fetch(`${ip}/api/auth/token`, options)
      .then(r => r.json())
      .catch(error => { console.log(error); return false })

    if (!res || !res.message || res.message != 'ok') {
      RemoveToken();
      return false
    }
  }

  return true
}
