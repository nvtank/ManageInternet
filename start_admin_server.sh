#!/bin/bash

echo "=== CHẠy ỨNG DỤNG MANAGE INTERNET ==="
echo ""
echo "📌 LƯU Ý: Phải chạy Admin TRƯỚC để khởi động Server!"
echo ""

# Chạy Admin trong terminal riêng
echo "🖥️  Bước 1: Khởi động Admin (Server)..."
echo "   Username: admin"
echo "   Password: 1"
echo ""

cd /home/nvtank/year3/ki1/LTM/ManageInternet/Admin/src

# Biên dịch
javac -cp ".:library/*:library/JFreeChart/*" Main.java

if [ $? -eq 0 ]; then
    echo "✓ Biên dịch Admin thành công!"
    echo ""
    echo "Đang khởi động Admin..."
    java -cp ".:library/*:library/JFreeChart/*" Main &
    ADMIN_PID=$!
    
    echo "Admin đang chạy với PID: $ADMIN_PID"
    echo ""
    echo "⏳ Chờ 3 giây để Admin khởi động Server..."
    sleep 3
    
    echo ""
    echo "✅ Bây giờ bạn có thể chạy User!"
    echo "   Mở terminal mới và chạy: ./run_user.sh"
    echo ""
    echo "👉 Đăng nhập Admin với: admin / 1"
    echo "👉 Sau đó chạy User với: vuong|minh|tan|nam / 1"
    echo ""
    echo "Nhấn Ctrl+C để dừng Admin"
    
    wait $ADMIN_PID
else
    echo "✗ Lỗi biên dịch Admin!"
    exit 1
fi
