from django.urls import path

from . import views

# Paths are exposed without trailing slashes so they match exactly what the
# black-box fuzzer requests, including the parameterized detail route.
urlpatterns = [
    path("items", views.items),
    path("items/<int:item_id>", views.item_detail),
    path("login", views.login),
    path("secure", views.secure_check),
]
