$(async function () {
    let username = getParam('username')
    $("#username").val(username)

    if (await TokenIsVaild())
        window.location.href = ('/')

    $('.preloader').fadeOut(1000);
})
function getParam(param) {
    return new URLSearchParams(window.location.search).get(param);
}
$(".account-form").on("submit", async function (event) {
    event.preventDefault();
    const form = $(this)

    let username = form.find('input[id="username"]').val().toLowerCase()
    let password = form.find('input[id="password"]').val()

    // Loading wait to response
    $('.preloader').fadeIn(100);

    //Request to Backend
    let res = await PenguRequestAPI('POST', 'api/auth/login',
        {
            "body": {
                "username": username,
                "password": password,
            }
        },
        { "Content-Type": "application/json" }).then(r => r.json()).catch(error => { console.log(error); return false })

    //Hide loading
    $('.preloader').fadeOut(100);

    console.log("Checking login response:", res);
    // Bad Connection
    if (!res) {
        return Swal.fire("Có lỗi kết nối vui lòng thử lại.");
    }

    if (res.message && res.message == 'Username or password is wrong') {
        return Swal.fire({
            icon: 'error',
            title: 'Đăng nhập không thành công',
            text: 'Mật khẩu hoặc tài khoản không hợp lệ!',
        })

    }

    if (!res.token) {
        return Swal.fire("Có lỗi xảy ra vui lòng thử lại.");
    }

    //Not thing may be success
    // await Swal.fire({
    //     icon: 'success',
    //     title: 'Đăng nhập thành công'
    // })

    SetToken(res.token)

    // Redirect to /sign-in.html
    let redirect = getParam('redirect') || '/index.html';//tự định nghĩa getParam bên trên
    // console.log("window.location.search=", window.location.search);
    // console.log("Redirecting to:", redirect);//check
    // alert("Submit event triggered! Check console2."); //đặt alert ms check đc console của submit, vì n quá nhanh
    window.location.href = redirect;
});

//http://localhost:9595/sign-in.html?redirect=/dashboard.html
//getParam('redirect') sẽ trả về "/dashboard.html"

//truy cập trang cần đ nhập, http://localhost:9595/protected-page.html
//nếu chưa đnhạp, n chuyển sang http://localhost:9595/sign-in.html?redirect=/protected-page.html