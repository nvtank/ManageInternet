#!/bin/bash

echo "=== Chạy User Application ==="
echo ""
echo "⚠️  QUAN TRỌNG: Đảm bảo Admin đã chạy TRƯỚC!"
echo ""

cd /home/nvtank/year3/ki1/LTM/ManageInternet/User/src

echo "Bước 1: Biên dịch User..."
javac -cp ".:library/*" Main.java

if [ $? -eq 0 ]; then
    echo "✓ Biên dịch thành công!"
    echo ""
    echo "Bước 2: Khởi động User..."
    echo ""
    echo "Tài khoản có sẵn:"
    echo "  - vuong / 1 (30,000đ)"
    echo "  - minh / 1  (20,000đ)"
    echo "  - tan / 1   (10,000đ)"
    echo "  - nam / 1   (30,000đ)"
    echo ""
    java -cp ".:library/*" Main
else
    echo "✗ Lỗi biên dịch User!"
    exit 1
fi
